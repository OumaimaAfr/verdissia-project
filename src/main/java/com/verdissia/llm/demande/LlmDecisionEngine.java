package com.verdissia.llm.demande;

import com.verdissia.llm.dto.LLMDemandeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
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
}