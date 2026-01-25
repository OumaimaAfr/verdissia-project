package com.verdissia.llm.dto;

import java.util.List;
import java.util.Map;

public record LLMDemandeResult(
        Decision decision,
        double confidence,
        List<LlmError> errors,
        String notes,
        Map<String, Double> fieldConfidences
) {
    public enum Decision {
        APPROVE, REJECT, REVIEW
    }

    public record LlmError(
            String field,
            String code,
            String message
    ) {
    }
}
