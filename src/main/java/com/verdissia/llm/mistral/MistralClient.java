package com.verdissia.llm.mistral;

import com.verdissia.llm.LLMResponseStatus;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLException;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MistralClient {

    private final WebClient webClient;
    private final String model;
    private Duration timeout = null;
    private final ObjectMapper objectMapper;
    private final boolean mockMode;
    private final Random random;

    public MistralClient(
            WebClient.Builder builder,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model,
            @Value("${llm.timeout-seconds:20}") long timeoutSeconds,
            @Value("${llm.mock-mode:false}") boolean mockMode
    ) {
        // Initialiser le timeout en premier
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.mockMode = mockMode;
        this.random = new Random();
        
        try {
            // Configuration pour ignorer la validation SSL (développement uniquement)
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            
            // Configuration pour augmenter la limite de buffer
            ExchangeStrategies strategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(t -> t.sslContext(sslContext))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                    .responseTimeout(timeout)
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler((int) timeout.getSeconds(), TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler((int) timeout.getSeconds(), TimeUnit.SECONDS)));

            this.webClient = builder
                    .baseUrl(baseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .exchangeStrategies(strategies)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();
        } catch (SSLException e) {
            log.error("Failed to configure SSL context", e);
            throw new RuntimeException("Failed to configure SSL context", e);
        }
        
        this.model = model;
        this.objectMapper = new ObjectMapper();
        
        log.info("MistralClient initialized with baseUrl: {}, model: {}", baseUrl, model);
    }

    public String chat(String prompt) {
        log.info("Sending request to Mistral API with prompt length: {}", prompt.length());

        if (mockMode) {
            return generateMockResponse(prompt);
        }

        var body = new ChatRequest(
                model,
                List.of(new Message("user", prompt)),
                0.7,
                true
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

                        // ✅ Cas succès - lire d'abord en String pour validation
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(rawBody -> {
                                    log.info("Raw response from Mistral: {}", rawBody);
                                    try {
                                        // Essayer de parser en JSON
                                        ChatResponse chatResponse = parseChatResponse(rawBody);
                                        return Mono.just(chatResponse);
                                    } catch (Exception e) {
                                        log.error("Failed to parse response as JSON. Raw response: {}", rawBody);
                                        return Mono.error(new RuntimeException(
                                            "Mistral API returned non-JSON response (status: " + 
                                            clientResponse.statusCode() + "): " + rawBody));
                                    }
                                });
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

    record ChatRequest(String model, List<Message> messages, double temperature, boolean stream) {
    }

    record Message(String role, String content) {
    }

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    private ChatResponse parseChatResponse(String json) throws Exception {
        return objectMapper.readValue(json, ChatResponse.class);
    }

    private String generateMockResponse(String prompt) {
        log.info("Generating mock response for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));
        
        // Simuler un délai de traitement
        try {
            Thread.sleep(500 + random.nextInt(1000)); // 500ms à 1.5s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Déterminer le type de réponse en fonction du contenu du prompt
        LLMResponseStatus status = determineResponseStatus(prompt);
        
        return switch (status) {
            case OK -> generateOKResponse(prompt);
            case KO -> generateKOResponse(prompt);
            case REVIEW -> generateReviewResponse(prompt);
        };
    }

    private LLMResponseStatus determineResponseStatus(String prompt) {
        // Logique simple pour déterminer le type de réponse
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("urgent") || lowerPrompt.contains("prioritaire") || lowerPrompt.contains("immédiat")) {
            return LLMResponseStatus.OK;
        } else if (lowerPrompt.contains("refus") || lowerPrompt.contains("rejet") || lowerPrompt.contains("invalide")) {
            return LLMResponseStatus.KO;
        } else if (lowerPrompt.contains("vérification") || lowerPrompt.contains("validation") || lowerPrompt.contains("examen")) {
            return LLMResponseStatus.REVIEW;
        }
        
        // Aléatoire si pas de mots-clés
        int rand = random.nextInt(100);
        if (rand < 60) return LLMResponseStatus.OK;      // 60% OK
        else if (rand < 80) return LLMResponseStatus.REVIEW; // 20% REVIEW
        else return LLMResponseStatus.KO;                 // 20% KO
    }

    private String generateOKResponse(String prompt) {
        return """
            {
                "status": "APPROVED",
                "confidence": 0.92,
                "reasoning": "La demande respecte tous les critères d'éligibilité. Le score de confiance est élevé (>0.85).",
                "recommendation": "APPROUVER - Traitement immédiat recommandé",
                "risk_level": "LOW",
                "processing_time": "24h",
                "next_steps": [
                    "Vérification finale des documents",
                    "Validation par le superviseur",
                    "Mise en production du contrat"
                ]
            }
            """;
    }

    private String generateKOResponse(String prompt) {
        return """
            {
                "status": "REJECTED",
                "confidence": 0.95,
                "reasoning": "La demande ne respecte pas les critères minimum requis. Plusieurs points bloquants identifiés.",
                "recommendation": "REJETER - Demande non conforme",
                "risk_level": "HIGH",
                "rejection_reasons": [
                    "Documents incomplets",
                    "Critères d'éligibilité non remplis",
                    "Historique de crédit insuffisant"
                ],
                "suggestion": "Inviter le client à compléter son dossier dans 3 mois"
            }
            """;
    }

    private String generateReviewResponse(String prompt) {
        return """
            {
                "status": "REVIEW",
                "confidence": 0.72,
                "reasoning": "La demande présente des points nécessitant une attention particulière. Score intermédiaire.",
                "recommendation": "EXAMEN MANUEL - Validation humaine requise",
                "risk_level": "MEDIUM",
                "attention_points": [
                    "Revenus variables nécessitant justification",
                    "Historique récent de changements de situation",
                    "Montant de demande proche des limites"
                ],
                "required_documents": [
                    "Justificatifs de revenus sur 12 mois",
                    "Explications sur les variations de revenus",
                    "Déclaration patrimoniale complémentaire"
                ],
                "estimated_processing_time": "3-5 jours ouvrés"
            }
            """;
    }
}
