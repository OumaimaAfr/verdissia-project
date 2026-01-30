package com.verdissia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalLlmService {

    private final WebClient webClient;
    
    @Value("${external.llm.base-url:https://bot-backend-1-j9ii.onrender.com/api/chat/message}")
    private String externalLlmBaseUrl;

    public String callExternalLlm(String prompt) {
        log.info("Appel à l'API LLM externe: {}", externalLlmBaseUrl);
        log.info("Prompt envoyé: {}", prompt.substring(0, Math.min(200, prompt.length())));

        try {
            // Construire le corps de la requête selon le format attendu par votre API
            ExternalLlmRequest request = ExternalLlmRequest.builder()
                    .message(prompt)
                    .clientType("electricity") // Par défaut, peut être adapté selon le contrat
                    .build();

            String response = webClient.post()
                    .uri(externalLlmBaseUrl) // Adapter l'endpoint selon votre API
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            log.info("Réponse reçue de l'API externe: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'API LLM externe: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'appel à l'API LLM externe: " + e.getMessage(), e);
        }
    }

    // DTO pour la requête vers l'API externe
    private static class ExternalLlmRequest {
        private String message;
        private String clientType;

        public static ExternalLlmRequestBuilder builder() {
            return new ExternalLlmRequestBuilder();
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getClientType() {
            return clientType;
        }

        public void setClientType(String clientType) {
            this.clientType = clientType;
        }

        public static class ExternalLlmRequestBuilder {
            private String message;
            private String clientType;

            public ExternalLlmRequestBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ExternalLlmRequestBuilder clientType(String clientType) {
                this.clientType = clientType;
                return this;
            }

            public ExternalLlmRequest build() {
                ExternalLlmRequest request = new ExternalLlmRequest();
                request.message = this.message;
                request.clientType = this.clientType;
                return request;
            }
        }
    }
}
