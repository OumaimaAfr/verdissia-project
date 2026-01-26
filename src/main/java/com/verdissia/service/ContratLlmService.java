package com.verdissia.service;

import com.verdissia.llm.mock.LlmAnalysisResult;
import com.verdissia.llm.mock.MockLlmAnalyzer;
import com.verdissia.model.Contrat;
import com.verdissia.model.LlmDecision;
import com.verdissia.repository.ContratRepository;
import com.verdissia.repository.LlmDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
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
    private final MockLlmAnalyzer mockLlmAnalyzer;
    private final LlmDecisionRepository llmDecisionRepository;

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
            // Déclencher l'analyse LLM avec les règles de gestion
            LlmAnalysisResult result = mockLlmAnalyzer.analyzeContrat(contrat);
            
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
}
