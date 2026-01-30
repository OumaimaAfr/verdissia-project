package com.verdissia.service;

import com.verdissia.model.Contrat;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalLlmService {

    private final WebClient webClient;
    
    @Value("${external.llm.base-url:https://bot-backend-1-j9ii.onrender.com/api/verdissia}")
    private String externalLlmBaseUrl;

    public String callExternalLlm(Contrat contrat) {
        log.info("Appel à l'API LLM externe: {}", externalLlmBaseUrl);
        log.info("Contrat analysé: {} - {}", contrat.getId(), contrat.getNumeroContrat());

        try {
            // Construire le corps de la requête selon le format VerdissiaRequest
            VerdissiaRequest request = VerdissiaRequest.builder()
                    .email(contrat.getEmail())
                    .telephone(contrat.getTelephone())
                    .adresse(contrat.getVoieLivraison())
                    .typeEnergie(contrat.getTypeEnergie() != null ? String.valueOf(contrat.getTypeEnergie()) : "Electricité")
                    .prix(contrat.getPrix() != null ? contrat.getPrix().doubleValue() : 0.0)
                    .dateSignature(contrat.getDateSignature() != null ? 
                        contrat.getDateSignature().toLocalDate() : java.time.LocalDate.now())
                    .dateMiseEnService(contrat.getDateMiseEnService() != null ? 
                        contrat.getDateMiseEnService().toLocalDate() : null)
                    .consentement(contrat.getConsentementClient() != null ? 
                        contrat.getConsentementClient().toString() : "false")
                    .build();

            log.info("Requête envoyée: email={}, téléphone={}, typeEnergie={}, dateMiseEnService={}", 
                    request.getEmail(), request.getTelephone(), request.getTypeEnergie(), request.getDateMiseEnService());

            String response = webClient.post()
                    .uri(externalLlmBaseUrl) // Endpoint principal sans /analyze
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

    // DTO VerdissiaRequest pour l'API externe
    @Data
    public static class VerdissiaRequest {
        private String email;
        private String telephone;
        private String adresse;
        private String typeEnergie;
        private Double prix;
        private LocalDate dateSignature;
        private LocalDate dateMiseEnService;
        private String consentement;

        public static VerdissiaRequestBuilder builder() {
            return new VerdissiaRequestBuilder();
        }

        public static class VerdissiaRequestBuilder {
            private String email;
            private String telephone;
            private String adresse;
            private String typeEnergie;
            private Double prix;
            private LocalDate dateSignature;
            private LocalDate dateMiseEnService;
            private String consentement;

            public VerdissiaRequestBuilder email(String email) {
                this.email = email;
                return this;
            }

            public VerdissiaRequestBuilder telephone(String telephone) {
                this.telephone = telephone;
                return this;
            }

            public VerdissiaRequestBuilder adresse(String adresse) {
                this.adresse = adresse;
                return this;
            }

            public VerdissiaRequestBuilder typeEnergie(String typeEnergie) {
                this.typeEnergie = typeEnergie;
                return this;
            }

            public VerdissiaRequestBuilder prix(Double prix) {
                this.prix = prix;
                return this;
            }

            public VerdissiaRequestBuilder dateSignature(LocalDate dateSignature) {
                this.dateSignature = dateSignature;
                return this;
            }

            public VerdissiaRequestBuilder dateMiseEnService(LocalDate dateMiseEnService) {
                this.dateMiseEnService = dateMiseEnService;
                return this;
            }

            public VerdissiaRequestBuilder consentement(String consentement) {
                this.consentement = consentement;
                return this;
            }

            public VerdissiaRequest build() {
                VerdissiaRequest request = new VerdissiaRequest();
                request.email = this.email;
                request.telephone = this.telephone;
                request.adresse = this.adresse;
                request.typeEnergie = this.typeEnergie;
                request.prix = this.prix;
                request.dateSignature = this.dateSignature;
                request.dateMiseEnService = this.dateMiseEnService;
                request.consentement = this.consentement;
                return request;
            }
        }
    }
}
