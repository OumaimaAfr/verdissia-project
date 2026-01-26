package com.verdissia.llm.demande;

import com.verdissia.llm.dto.LLMDemandeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LlmDecisionEngine {

    private final double approveThreshold;
    private final double rejectThreshold;

    public LlmDecisionEngine(
            @Value("${llm.threshold-auto-approve}") double approveThreshold,
            @Value("${llm.threshold-auto-reject}") double rejectThreshold
    ) {
        this.approveThreshold = approveThreshold;
        this.rejectThreshold = rejectThreshold;
    }

    public String decide(LLMDemandeResult r) {
        if (r.decision() == LLMDemandeResult.Decision.APPROVE && r.confidence() >= approveThreshold) {
            return "OK";
        }
        if (r.decision() == LLMDemandeResult.Decision.REJECT && r.confidence() <= rejectThreshold) {
            return "KO";
        }
        return "REVIEW";
    }

    public boolean analyzeContrat(String contratData) {
        log.info("Analyse LLM du contrat avec les données: {}", contratData);
        
        // TODO: Implémenter la logique d'analyse LLM réelle
        // Pour l'instant, simulation basique basée sur quelques règles
        
        // Simulation: validation basique du contrat
        if (contratData.contains("test") || contratData.contains("demo")) {
            log.warn("Contrat de test/détecté, rejet automatique");
            return false;
        }
        
        // Simulation: vérification si le prix est raisonnable (basé sur la présence dans les données)
        if (contratData.contains("Prix: 0") || contratData.contains("Prix: null")) {
            log.warn("Prix invalide détecté");
            return false;
        }
        
        // Simulation: vérification du consentement client
        if (!contratData.contains("Consentement client: true")) {
            log.warn("Consentement client manquant");
            return false;
        }
        
        log.info("Contrat validé avec succès par l'analyse LLM");
        return true;
    }
}
