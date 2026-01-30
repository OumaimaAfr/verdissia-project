package com.verdissia.controller;

import com.verdissia.dto.request.MistralRequest;
import com.verdissia.dto.response.MistralResponse;
import com.verdissia.service.MistralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/mistral")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MistralController {

    private final MistralService mistralService;

    @PostMapping("/prompt")
    public Mono<ResponseEntity<MistralResponse>> callWithPrompt(@Valid @RequestBody PromptRequest request) {
        log.info("=== Direct Mistral API call ===");
        log.info("System Prompt: {}", request.getSystemPrompt());
        log.info("User Message: {}", request.getUserMessage());
        log.info("Model: {}", request.getModel());
        log.info("Max Tokens: {}", request.getMaxTokens());
        log.info("Temperature: {}", request.getTemperature());

        MistralRequest mistralRequest = MistralRequest.builder()
                .model(request.getModel() != null ? request.getModel() : "mistral-small-latest")
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1000)
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .messages(List.of(
                        MistralRequest.Message.builder()
                                .role("system")
                                .content(request.getSystemPrompt())
                                .build(),
                        MistralRequest.Message.builder()
                                .role("user")
                                .content(request.getUserMessage())
                                .build()
                ))
                .build();

        return mistralService.callDirect(mistralRequest)
                .doOnSuccess(response -> log.info("Successfully received direct Mistral response"))
                .doOnError(error -> log.error("Error in direct Mistral call: {}", error.getMessage(), error))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("!!! Mistral Controller Error Handler Triggered !!!");
                    log.error("Error type: {}", error.getClass().getName());
                    log.error("Error message: {}", error.getMessage(), error);

                    return Mono.just(ResponseEntity.internalServerError()
                            .body(MistralResponse.builder()
                                    .error("Erreur lors de l'appel à Mistral: " + error.getMessage())
                                    .build()));
                });
    }

    @PostMapping("/simple")
    public Mono<ResponseEntity<String>> simplePrompt(@Valid @RequestBody SimplePromptRequest request) {
        log.info("=== Simple Mistral prompt ===");
        log.info("Prompt: {}", request.getPrompt());

        return mistralService.generateResponse(request.getPrompt(), "general")
                .doOnSuccess(response -> log.info("Simple prompt completed successfully"))
                .doOnError(error -> log.error("Error in simple prompt: {}", error.getMessage(), error))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("!!! Simple Prompt Error Handler Triggered !!!");
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("Erreur lors du traitement du prompt: " + error.getMessage()));
                });
    }

    // DTOs pour les requêtes
    public static class PromptRequest {
        private String systemPrompt;
        private String userMessage;
        private String model;
        private Integer maxTokens;
        private Double temperature;

        // Getters et Setters
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
    }

    public static class SimplePromptRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }
}
