package com.verdissia.service;

import com.verdissia.llm.mock.LlmAnalysisResult;
import com.verdissia.model.Contrat;
import com.verdissia.model.LlmDecision;
import com.verdissia.repository.ContratRepository;
import com.verdissia.repository.LlmDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContratLlmService {

    private final ContratRepository contratRepository;
    private final ExternalLlmService externalLlmService;
    private final LlmDecisionRepository llmDecisionRepository;
    private final PromptTemplate promptTemplate;

    @Scheduled(fixedRate = 30000) // Exécuter toutes les 30 secondes
    public void processPendingContrats() {
        log.info("Recherche des contrats en attente d'analyse LLM...");
        
        List<Contrat> pendingContrats = contratRepository.findByStatutLlm(Contrat.StatutLlm.PENDING);
        
        if (pendingContrats.isEmpty()) {
            log.info("Aucun contrat en attente d'analyse LLM");
            return;
        }
        
        log.info("Trouvé {} contrat(s) en attente d'analyse LLM", pendingContrats.size());
        
        for (Contrat contrat : pendingContrats) {
            try {
                processContrat(contrat);
            } catch (Exception e) {
                log.error("Erreur lors du traitement du contrat {} : {}", contrat.getId(), e.getMessage(), e);
            }
        }
    }
    
    private void processContrat(Contrat contrat) {
        log.info("Début de l'analyse LLM pour le contrat {} - {}", contrat.getId(), contrat.getNumeroContrat());
        
        LlmDecision llmDecision = LlmDecision.builder()
                .contratId(contrat.getId())
                .processStatus(LlmDecision.ProcessStatus.PENDING)
                .build();
        
        try {
            // Déclencher l'analyse LLM avec l'API externe
            log.info("Envoi du contrat à l'API externe pour analyse: {}", contrat.getId());
            
            String externalResponse = externalLlmService.callExternalLlm(contrat);
            log.info("Réponse brute de l'API externe pour contrat {}: {}", contrat.getId(), externalResponse);
            
            LlmAnalysisResult result = parseExternalResponse(externalResponse);
            log.info("Résultat parsé pour contrat {}: decision={}, motifCode={}, confidence={}", 
                    contrat.getId(), result.getDecision(), result.getMotifCode(), result.getConfidence());
            
            // Mettre à jour le statut du contrat
            contrat.setStatutLlm(Contrat.StatutLlm.TRAITE);
            contratRepository.save(contrat);
            
            // Sauvegarder le résultat dans la table llm_decision
            llmDecision.setDecision(result.getDecision());
            llmDecision.setMotifCode(result.getMotifCode());
            llmDecision.setMotifMessage(result.getMotif());
            llmDecision.setConfidence(result.getConfidence());
            llmDecision.setActionConseiller(result.getActionConseiller());
            llmDecision.setDetails(result.getDetails());
            llmDecision.setProcessStatus(LlmDecision.ProcessStatus.SUCCESS);
            
            llmDecisionRepository.save(llmDecision);
            
            // Logger le résultat détaillé
            log.info("Analyse LLM terminée pour le contrat {} - Décision: {} - Motif: {} - Action conseiller: {} - Confidence: {}", 
                    contrat.getId(), result.getDecision(), result.getMotif(), result.getActionConseiller(), result.getConfidence());
            
            // Logger les détails pour le conseiller
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                log.info("Détails pour le conseiller - Contrat {}: {}", contrat.getId(), result.getDetails());
            }
            
        } catch (Exception e) {
            log.error("Échec de l'analyse LLM pour le contrat {} : {}", contrat.getId(), e.getMessage(), e);
            
            // Mettre à jour le statut d'erreur dans llm_decision
            llmDecision.setDecision("ERROR");
            llmDecision.setMotifCode("ANALYSIS_ERROR");
            llmDecision.setMotifMessage("Erreur lors de l'analyse LLM");
            llmDecision.setProcessStatus(LlmDecision.ProcessStatus.ERROR);
            llmDecision.setErrorMessage(e.getMessage());
            
            llmDecisionRepository.save(llmDecision);
            throw e;
        }
    }
    
    public Contrat getContratByEmail(String email) {
        Optional<Contrat> contrat = contratRepository.findByEmail(email);
        return contrat.orElse(null);
    }
    
    public List<Contrat> getAllContrats() {
        return contratRepository.findAll();
    }
    
    private LlmAnalysisResult parseExternalResponse(String response) {
        log.info("Tentative de parsing de la réponse externe: {}", response);
        
        try {
            // Parser la réponse de l'API externe
            tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
            ExternalApiResponse apiResponse = mapper.readValue(response, ExternalApiResponse.class);
            
            log.info("Réponse API externe - success: {}, reference: {}", 
                    apiResponse.isSuccess(), apiResponse.getReference());
            
            if (!apiResponse.isSuccess()) {
                log.error("L'API externe a retourné une erreur: {}", apiResponse.getMessage());
                
                // Extraire les informations du message
                String message = apiResponse.getMessage();
                log.info("Message à parser: {}", message);
                
                String motifMessage = extractMotifMessage(message);
                String actionConseiller = extractActionConseillerFromMessage(message);
                String details = extractDetailsFromMessage(message);
                String motifCode = extractMotifCodeFromMessage(message);
                
                log.info("Résultats extraction - motifMessage: {}, actionConseiller: {}, motifCode: {}", 
                        motifMessage, actionConseiller, motifCode);
                
                return LlmAnalysisResult.builder()
                        .decision("REJET") // Si success=false, c'est un rejet
                        .motifCode(motifCode)
                        .motif(motifMessage)
                        .actionConseiller(actionConseiller)
                        .details(details)
                        .confidence(new java.math.BigDecimal("0.75"))
                        .build();
            }
            
            // Extraire le JSON du message (il est encapsulé dans ```json\n...```)
            String message = apiResponse.getMessage();
            if (message.contains("```json")) {
                int start = message.indexOf("```json") + 7;
                int end = message.indexOf("```", start);
                if (end > start) {
                    String jsonContent = message.substring(start, end).trim();
                    log.info("JSON extrait: {}", jsonContent);
                    
                    // Parser le contenu JSON de Mistral
                    MistralResponseDTO mistralResponse = mapper.readValue(jsonContent, MistralResponseDTO.class);
                    
                    return LlmAnalysisResult.builder()
                            .decision(mistralResponse.getDecision())
                            .motifCode(mistralResponse.getMotifCode())
                            .motif(mistralResponse.getMotif())
                            .actionConseiller(mistralResponse.getActionConseiller())
                            .details(mistralResponse.getDetails())
                            .confidence(mistralResponse.getConfidence())
                            .build();
                }
            }
            
            // Si on ne peut pas extraire le JSON, utiliser le message comme motif
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("PARSING_ERROR")
                    .motif("Impossible d'extraire la décision de la réponse")
                    .actionConseiller("EXAMINER")
                    .details("Réponse brute: " + message)
                    .confidence(new java.math.BigDecimal("0.50"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Erreur parsing réponse externe - Response: {} - Error: {}", response, e.getMessage(), e);
            
            // En cas d'erreur de parsing, retourner une réponse par défaut
            return LlmAnalysisResult.builder()
                    .decision("ERROR")
                    .motifCode("PARSING_ERROR")
                    .motif("Erreur lors de l'analyse de la réponse externe")
                    .actionConseiller("ERREUR TECHNIQUE")
                    .details("Réponse externe non valide: " + response)
                    .confidence(java.math.BigDecimal.ZERO)
                    .build();
        }
    }
    
    // DTO pour la réponse de l'API externe
    private static class ExternalApiResponse {
        private boolean success;
        private String message;
        private String reference;
        private long timestamp;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    // DTO pour la réponse de Mistral
    private static class MistralResponseDTO {
        private String decision;
        private String motifCode;
        private String motif;
        private String actionConseiller;
        private String details;
        private java.math.BigDecimal confidence;
        
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        
        public String getMotifCode() { return motifCode; }
        public void setMotifCode(String motifCode) { this.motifCode = motifCode; }
        
        public String getMotif() { return motif; }
        public void setMotif(String motif) { this.motif = motif; }
        
        public String getActionConseiller() { return actionConseiller; }
        public void setActionConseiller(String actionConseiller) { this.actionConseiller = actionConseiller; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public java.math.BigDecimal getConfidence() { return confidence; }
        public void setConfidence(java.math.BigDecimal confidence) { this.confidence = confidence; }
    }
    
    // Méthodes d'extraction du message
    private String extractMotifMessage(String message) {
        log.info("Extraction motif_message du message LLM: {}", message);
        
        String code = extractMotifCodeFromMessage(message);

        return code;
    }
    
    private String extractDetailsFromMessage(String message) {
        log.info("Extraction details du message LLM: {}", message);
        
        // Extraire tout après "Contrat rejet: " mais enlever "Action conseillée: ..."
        if (message.contains("Contrat rejet:")) {
            int start = message.indexOf("Contrat rejet:") + 14;
            String details = message.substring(start).trim();
            
            // Enlever la partie "Action conseillée: ..." de la fin
            if (details.contains("Action conseillée:")) {
                int end = details.indexOf("Action conseillée:");
                details = details.substring(0, end).trim();
                // Enlever les ".." ou ". " de la fin s'ils existent
                if (details.endsWith("..")) {
                    details = details.substring(0, details.length() - 2).trim();
                } else if (details.endsWith(".")) {
                    details = details.substring(0, details.length() - 1).trim();
                }
            }
            
            log.info("Details extraits: {}", details);
            return details;
        }
        return message;
    }
    
    private String extractMotifCodeFromMessage(String message) {
        log.info("Extraction motif_code du message LLM: {}", message);
        
        // Chercher "Contrat rejet: " et extraire le motif principal
        if (message.contains("Contrat rejet:")) {
            int start = message.indexOf("Contrat rejet:") + 14;
            int end = message.indexOf("..", start);
            if (end > start) {
                String motifCode = message.substring(start, end).trim();
                // Nettoyer et créer un code court
                if (motifCode.toLowerCase().contains("téléphone")) {
                    return "Numéro de téléphone Invalide";
                } else if (motifCode.toLowerCase().contains("adresse")) {
                    return "Adresse Invalide";
                } else if (motifCode.toLowerCase().contains("email")) {
                    return "Email Invalide";
                } else if (motifCode.toLowerCase().contains("date")) {
                    return "Date incohérente";
                } else if (motifCode.toLowerCase().contains("consentement")) {
                    return "Consentement manquant";
                }
                log.info("Motif_code extrait: {}", motifCode);
                return "Contrat à revoir";
            }
        }
        return "Contrat à revoir";
    }
    
    private String extractActionConseillerFromMessage(String message) {
        log.info("Extraction action_conseiller du message LLM: {}", message);
        
        // Chercher "Action conseillée: " ou "Action conseillée:"
        if (message.contains("Action conseillée:")) {
            int start = message.indexOf("Action conseillée:") + 18;
            int end = message.length();
            // Chercher la fin de la phrase
            int nextDot = message.indexOf(".", start);
            if (nextDot > start && nextDot < start + 50) {
                end = nextDot;
            }
            String action = message.substring(start, end).trim();
            log.info("Action_conseiller extraite (méthode 1): {}", action);
            return action;
        }
        // Alternative: chercher "Action conseillée:"
        if (message.contains("Action conseillée:")) {
            int start = message.indexOf("Action conseillée:") + 18;
            int end = message.length();
            int nextDot = message.indexOf(".", start);
            if (nextDot > start && nextDot < start + 50) {
                end = nextDot;
            }
            String action = message.substring(start, end).trim();
            log.info("Action_conseiller extraite (méthode 2): {}", action);
            return action;
        }
        String defaultAction = "EXAMINER";
        log.info("Action_conseiller par défaut utilisée: {}", defaultAction);
        return defaultAction; // Valeur par défaut
    }
    
    private String extractDecisionFromMessage(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("contrat rejet") || lowerMessage.contains("rejet")) {
            return "REJET";
        } else if (lowerMessage.contains("contrat valide") || lowerMessage.contains("validé") || lowerMessage.contains("approuvé")) {
            return "VALIDE";
        }
        return "REJET"; // Par défaut
    }
}
