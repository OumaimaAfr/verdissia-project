package com.verdissia.llm;

import com.verdissia.llm.dto.LLMDemandeInput;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LLMPromptFactory {

    private final tools.jackson.databind.ObjectMapper mapper;

    public LLMPromptFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String buildPrompt(LLMDemandeInput input) {
        try {
            String inputJson = mapper.writeValueAsString(input);

            return """
You are a strict validator for energy subscription requests in France.
Return ONLY valid JSON. No markdown. No explanations.

Allowed values:
- typeEnergy: ["GAZ","ELECTRICITE","DUAL"]
- preferenceOffre: ["PRIX","STABILITE","ENERGIE_VERTE","INDIFFERENT"]
- typeDemande: ["SOUSCRIPTION"]

Rules:
- consentementClient must be true to approve
- email must be valid
- telephone must look like a French number
- dateMiseService must be YYYY-MM-DD and not in the past
- supplyInfo.typeEnergy must match offerContext.offreTypeEnergy
- supplyInfo.offreId must equal offerContext.offreId

Output JSON schema:
{
"decision": "APPROVE|REJECT|REVIEW",
"confidence": 0.0,
"errors": [{"field":"...","code":"...","message":"..."}],
"notes": "optional",
"fieldConfidences": {"field": 0.0}
}

Input:
%s
""".formatted(inputJson);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot build LLM prompt", e);
        }
    }
}

