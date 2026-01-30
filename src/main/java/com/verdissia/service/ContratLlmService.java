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
            String prompt = promptTemplate.buildContratAnalysisPrompt(contrat);
            log.info("Envoi du prompt à l'API externe pour contrat {}: {}", contrat.getId(), prompt);
            
            String externalResponse = externalLlmService.callExternalLlm(prompt);
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
            // Si la réponse est une simple chaîne de caractères, la convertir en format structuré
            if (response.startsWith("\"") && response.endsWith("\"")) {
                response = response.substring(1, response.length() - 1);
            }
            
            // Créer un résultat basé sur la réponse texte de l'API
            return LlmAnalysisResult.builder()
                    .decision(determineDecisionFromResponse(response))
                    .motifCode(determineMotifCodeFromResponse(response))
                    .motif(response)
                    .actionConseiller(determineActionFromResponse(response))
                    .details("Analyse par API externe: " + response)
                    .confidence(calculateConfidenceFromResponse(response))
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
    
    private String determineDecisionFromResponse(String response) {
        String lowerResponse = response.toLowerCase();
        if (lowerResponse.contains("valide") || lowerResponse.contains("approuve") || lowerResponse.contains("accepte")) {
            return "VALIDE";
        } else if (lowerResponse.contains("rejet") || lowerResponse.contains("invalide") || lowerResponse.contains("refuse")) {
            return "REJET";
        } else {
            return "VALIDE"; // Par défaut
        }
    }
    
    private String determineMotifCodeFromResponse(String response) {
        String decision = determineDecisionFromResponse(response);
        if ("VALIDE".equals(decision)) {
            return "CONTRACT_VALID";
        } else {
            return "MANUAL_REVIEW";
        }
    }
    
    private String determineActionFromResponse(String response) {
        String decision = determineDecisionFromResponse(response);
        if ("VALIDE".equals(decision)) {
            return "TRAITER";
        } else {
            return "EXAMINER";
        }
    }
    
    private java.math.BigDecimal calculateConfidenceFromResponse(String response) {
        // Calculer un score de confiance basé sur la longueur et le contenu de la réponse
        if (response.length() > 100) {
            return new java.math.BigDecimal("0.85");
        } else if (response.length() > 50) {
            return new java.math.BigDecimal("0.75");
        } else {
            return new java.math.BigDecimal("0.65");
        }
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
