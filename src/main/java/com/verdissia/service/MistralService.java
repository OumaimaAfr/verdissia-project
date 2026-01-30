package com.verdissia.service;

import com.verdissia.dto.request.MistralRequest;
import com.verdissia.dto.response.MistralResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MistralService {

    private final WebClient webClient;
    private final com.verdissia.config.MistralConfig mistralConfig;

    public Mono<String> generateResponse(String userMessage, String clientType) {
        String systemPrompt = buildSystemPrompt(clientType);

        MistralRequest request = MistralRequest.builder()
                .model(mistralConfig.getModel())
                .maxTokens(mistralConfig.getMaxTokens())
                .temperature(mistralConfig.getTemperature())
                .messages(List.of(
                        MistralRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        MistralRequest.Message.builder()
                                .role("user")
                                .content(userMessage)
                                .build()
                ))
                .build();

        log.info("Sending request to Mistral API with model: {}, URL: {}", mistralConfig.getModel(), mistralConfig.getUrl());
        log.info("API Key configured: {}", mistralConfig.getKey() != null && !mistralConfig.getKey().isEmpty() ? "Yes (masked: " + mistralConfig.getKey().substring(0, Math.min(10, mistralConfig.getKey().length())) + "...)" : "NO - MISSING!");
        log.debug("Request details: {}", request);

        return webClient.post()
                .uri(mistralConfig.getUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mistralConfig.getKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.warn("Received error status from Mistral API: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("Mistral API error - Status: {}, Body: {}", response.statusCode().value(), body);
                                return Mono.error(new RuntimeException("Mistral API error [" + response.statusCode().value() + "]: " + body));
                            });
                })
                .bodyToMono(MistralResponse.class)
                .doOnNext(response -> log.info("Successfully parsed Mistral response"))
                .map(response -> {
                    log.info("Received response from Mistral API successfully");
                    log.debug("Response details: {}", response);
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        log.debug("Extracted content: {}", content);
                        return content;
                    }
                    log.warn("No choices in Mistral response");
                    return "Je suis désolé, je n'ai pas pu générer une réponse appropriée.";
                })
                .doOnError(error -> {
                    log.error("!!! ERROR in Mistral API call !!!");
                    log.error("Error type: {}", error.getClass().getName());
                    log.error("Error message: {}", error.getMessage());
                    log.error("Full stack trace:", error);
                })
                .doOnSuccess(result -> log.info("Mistral service completed successfully with result length: {}", result != null ? result.length() : 0));
    }

    public Mono<MistralResponse> callDirect(MistralRequest request) {
        log.info("Sending direct request to Mistral API with model: {}, URL: {}", request.getModel(), mistralConfig.getUrl());
        log.info("API Key configured: {}", mistralConfig.getKey() != null && !mistralConfig.getKey().isEmpty() ? "Yes (masked: " + mistralConfig.getKey().substring(0, Math.min(10, mistralConfig.getKey().length())) + "...)" : "NO - MISSING!");
        log.debug("Request details: {}", request);

        return webClient.post()
                .uri(mistralConfig.getUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mistralConfig.getKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.warn("Received error status from Mistral API: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("Mistral API error - Status: {}, Body: {}", response.statusCode().value(), body);
                                return Mono.error(new RuntimeException("Mistral API error [" + response.statusCode().value() + "]: " + body));
                            });
                })
                .bodyToMono(MistralResponse.class)
                .doOnNext(response -> log.info("Successfully parsed direct Mistral response"))
                .doOnError(error -> {
                    log.error("!!! ERROR in direct Mistral API call !!!");
                    log.error("Error type: {}", error.getClass().getName());
                    log.error("Error message: {}", error.getMessage());
                    log.error("Full stack trace:", error);
                });
    }

    private String buildSystemPrompt(String clientType) {
        String basePrompt = """
                Vous êtes un assistant virtuel spécialisé dans le secteur de l'énergie, particulièrement pour les clients %s.
                
                Votre rôle est d'aider les clients avec leurs questions concernant:
                - Les factures et la facturation
                - Les contrats et tarifs
                - Les pannes et incidents
                - Les démarches administratives
                - Les conseils d'économie d'énergie
                - Les services client
                
                Instructions importantes:
                1. Répondez toujours en français
                2. Soyez professionnel, courtois et empathique
                3. Donnez des informations précises et utiles
                4. Si vous ne connaissez pas une information spécifique, orientez le client vers le service client approprié
                5. Proposez des solutions concrètes quand c'est possible
                6. Restez dans le domaine de l'énergie (%s)
                
                Répondez de manière concise mais complète.
                """;

        String energyType = "electricity".equals(clientType) ? "électricité" : "gaz";
        return String.format(basePrompt, energyType, energyType);
    }
}
