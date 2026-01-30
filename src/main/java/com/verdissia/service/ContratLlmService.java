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
                return LlmAnalysisResult.builder()
                        .decision("ERROR")
                        .motifCode("EXTERNAL_API_ERROR")
                        .motif("Erreur de l'API externe: " + apiResponse.getMessage())
                        .actionConseiller("ERREUR TECHNIQUE")
                        .details("Référence: " + apiResponse.getReference() + " - " + apiResponse.getMessage())
                        .confidence(java.math.BigDecimal.ZERO)
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
    
    // DTO pour parser la réponse de l'API externe
    private static class ExternalLlmResponseDTO {
        private String decision;
        private String motifCode;
        private String motif;
        private String actionConseiller;
        private String details;
        private java.math.BigDecimal confidence;
        
        // Getters et Setters
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
}
