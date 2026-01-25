package com.verdissia.llm.service;


import com.verdissia.llm.LLMPromptFactory;
import com.verdissia.llm.dto.LLMDemandeInput;
import com.verdissia.llm.dto.LLMDemandeResult;
import com.verdissia.llm.mistral.MistralClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class LLMValidationService {

    private final MistralClient client;
    private final LLMPromptFactory promptFactory;
    private final ObjectMapper mapper;

    public LLMValidationService(MistralClient client, LLMPromptFactory promptFactory, ObjectMapper mapper) {
        this.client = client;
        this.promptFactory = promptFactory;
        this.mapper = mapper;
    }

    public LLMDemandeResult validate(LLMDemandeInput input) {
        String prompt = promptFactory.buildPrompt(input);
        String raw = client.chat(prompt);

        try {
            return mapper.readValue(raw, LLMDemandeResult.class);
        } catch (Exception e) {
            return new LLMDemandeResult(
                    LLMDemandeResult.Decision.REVIEW,
                    0.0,
                    List.of(new LLMDemandeResult.LlmError(
                            "llm", "INVALID_JSON", "LLM response was not valid JSON"
                    )),
                    raw,
                    Map.of()
            );
        }
    }
}
