package com.verdissia.llm.mistral;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class MistralClient {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public MistralClient(
            WebClient.Builder builder,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model,
            @Value("${llm.timeout-seconds:20}") long timeoutSeconds
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        
        log.info("MistralClient initialized with baseUrl: {}, model: {}", baseUrl, model);
    }

    public String chat(String prompt) {
        log.info("Sending request to Mistral API with prompt length: {}", prompt.length());

        var body = new ChatRequest(
                model,
                List.of(new Message("user", prompt)),
                0.2
        );

        try {

            ChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchangeToMono(clientResponse -> {

                        log.info("Mistral HTTP status: {}", clientResponse.statusCode());

                        // ✅ Cas redirection (3xx)
                        if (clientResponse.statusCode().is3xxRedirection()) {
                            String location = clientResponse.headers().asHttpHeaders().getFirst("Location");
                            log.error("Mistral API redirection to: {}", location);
                            return Mono.error(
                                    new RuntimeException(
                                            "Mistral API redirection "
                                                    + clientResponse.statusCode()
                                                    + " to: " + location
                                    )
                            );
                        }

                        // ✅ Cas erreur HTTP (4xx / 5xx)
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(errorBody -> {
                                        log.error("Mistral error body: {}", errorBody);
                                        return Mono.error(
                                                new RuntimeException(
                                                        "Mistral API error "
                                                                + clientResponse.statusCode()
                                                                + " : " + errorBody
                                                )
                                        );
                                    });
                        }

                        // ✅ Cas succès
                        return clientResponse.bodyToMono(ChatResponse.class);
                    })
                    .timeout(timeout)
                    .block();


            log.info("Received response from Mistral: {}", response);

            if (response == null) {
                log.error("Response is null");
                throw new IllegalStateException("Empty response from Mistral - response is null");
            }
            
            if (response.choices() == null || response.choices().isEmpty()) {
                log.error("Response choices is null or empty: {}", response.choices());
                throw new IllegalStateException("Empty response from Mistral - no choices in response");
            }
            
            var content = response.choices().get(0).message().content();
            log.info("Extracted content length: {}", content != null ? content.length() : 0);
            
            return content;

        } catch (Exception e) {
            log.error("Exception during Mistral API call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Mistral API: " + e.getMessage(), e);
        }
    }

    record ChatRequest(String model, List<Message> messages, double temperature) {
    }

    record Message(String role, String content) {
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }
}
