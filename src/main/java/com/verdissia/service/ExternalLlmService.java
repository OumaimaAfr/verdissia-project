package com.verdissia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.verdissia.llm.mistral.MistralClient;
import com.verdissia.model.Contrat;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalLlmService {

    private final MistralClient mistralClient;
    private final PromptTemplate promptTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public String callExternalLlm(Contrat contrat) {
        log.info("Appel à l'API Mistral (direct)");
        log.info("Contrat analysé: {} - {}", contrat.getId(), contrat.getNumeroContrat());

        try {
            // Construire le corps de la requête selon le format VerdissiaRequest
            VerdissiaRequest request = VerdissiaRequest.builder()
                    .email(contrat.getEmail())
                    .telephone(contrat.getTelephone())
                    .adresse(contrat.getVoieLivraison() + ", " + contrat.getCodePostalLivraison() + " " + contrat.getVilleLivraison())
                    .typeEnergie(convertTypeEnergie(contrat.getTypeEnergie()))
                    .prix(contrat.getPrix() != null ? contrat.getPrix().doubleValue() : 0.0)
                    .dateSignature(contrat.getDateSignature() != null ? 
                        contrat.getDateSignature().toLocalDate() : java.time.LocalDate.now().minusDays(1))
                    .dateMiseEnService(contrat.getDateMiseEnService() != null ? 
                        contrat.getDateMiseEnService().toLocalDate() : java.time.LocalDate.now().plusDays(3))
                    .consentement(contrat.getConsentementClient() != null ? 
                        contrat.getConsentementClient().toString() : "false")
                    .build();

            log.info("Requête envoyée: email={}, téléphone={}, typeEnergie={}, prix={}, dateMiseEnService={}, Adresse={}",
                    request.getEmail(), request.getTelephone(), request.getTypeEnergie(), request.getPrix(), request.getDateMiseEnService(), request.getAdresse());
            
            // Log du JSON complet envoyé pour debug
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("JSON complet envoyé à Mistral (debug): {}", requestJson);
            } catch (Exception jsonEx) {
                log.warn("Impossible de sérialiser la requête en JSON pour le log");
            }

            String prompt = promptTemplate.buildContratAnalysisPrompt(contrat);
            String mistralContent = mistralClient.chat(prompt);
            log.info("Réponse reçue de Mistral (content): {}", mistralContent);

            String jsonContent = normalizeMistralContentToJson(mistralContent);
            JsonNode analysisNode = objectMapper.readTree(jsonContent);

            String decision = analysisNode.path("decision").asText(null);
            String motifCode = analysisNode.path("motifCode").asText(null);
            String motif = analysisNode.path("motif").asText("");
            String actionConseiller = analysisNode.path("actionConseiller").asText("");
            String details = normalizeDetailsToSingleLine(analysisNode.path("details").asText(""));

            ObjectNode overridden = applyGeographicAddressOverride(contrat, analysisNode);
            if (overridden != null) {
                decision = overridden.path("decision").asText(null);
                motifCode = overridden.path("motifCode").asText(null);
                motif = overridden.path("motif").asText("");
                actionConseiller = overridden.path("actionConseiller").asText("");
                details = normalizeDetailsToSingleLine(overridden.path("details").asText(""));
                jsonContent = objectMapper.writeValueAsString(overridden);
            }

            boolean success = decision != null && !"REJET".equalsIgnoreCase(decision);

            String reference = "MISTRAL-" + contrat.getId();
            long timestamp = System.currentTimeMillis();

            String message;
            if (success) {
                message = "Contrat valide: " + motif + ".. Action conseillée: " + actionConseiller + "\n\n```json\n" + jsonContent + "\n```";
            } else {
                String code = motifCode != null && !motifCode.isBlank() ? motifCode : "REJET";
                String rejectionDetails = details != null && !details.isBlank() ? details : motif;
                message = "Contrat rejet: " + code + ".. " + rejectionDetails + ".. Action conseillée: " + actionConseiller;
            }

            String wrappedResponse = "{" +
                    "\"success\":" + success + "," +
                    "\"message\":" + objectMapper.writeValueAsString(message) + "," +
                    "\"reference\":" + objectMapper.writeValueAsString(reference) + "," +
                    "\"timestamp\":" + timestamp +
                    "}";

            log.info("Réponse (wrapper) renvoyée au parsing existant: {}", wrappedResponse);
            return wrappedResponse;

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à Mistral: {}", e.getMessage(), e);
            return "{\"success\":false,\"message\":\"Service LLM indisponible - veuillez réessayer plus tard\",\"reference\":\"SYSTEM-ERROR\",\"timestamp\":" + System.currentTimeMillis() + "}";
        }
    }

    private String normalizeMistralContentToJson(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Mistral content is null");
        }

        String trimmed = content.trim();
        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```json");
            if (start >= 0) {
                start = start + 7;
                int end = trimmed.indexOf("```", start);
                if (end > start) {
                    return trimmed.substring(start, end).trim();
                }
            }

            int anyFenceStart = trimmed.indexOf("```");
            if (anyFenceStart >= 0) {
                int after = anyFenceStart + 3;
                if (after < trimmed.length() && trimmed.charAt(after) == '\n') {
                    after++;
                }
                int end = trimmed.indexOf("```", after);
                if (end > after) {
                    return trimmed.substring(after, end).trim();
                }
            }
        }

        return trimmed;
    }

    private String normalizeDetailsToSingleLine(String details) {
        if (details == null) {
            return "";
        }

        String normalized = details
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
    }

    private ObjectNode applyGeographicAddressOverride(Contrat contrat, JsonNode analysisNode) {
        if (contrat == null) {
            return null;
        }

        String cp = contrat.getCodePostalLivraison();
        String ville = contrat.getVilleLivraison();
        if (cp == null || ville == null) {
            return null;
        }

        String cpTrim = cp.trim();
        String villeTrim = ville.trim();
        if (cpTrim.length() < 2 || villeTrim.isBlank()) {
            return null;
        }

        String prefix = cpTrim.substring(0, 2);
        boolean mismatch = false;
        if ("75".equals(prefix) && !villeTrim.equalsIgnoreCase("Paris")) {
            mismatch = true;
        }
        if ("69".equals(prefix) && !villeTrim.equalsIgnoreCase("Lyon")) {
            mismatch = true;
        }

        if (!mismatch) {
            return null;
        }

        ObjectNode o = objectMapper.createObjectNode();
        o.put("decision", "REJET");
        o.put("motifCode", "ADDRESS_INVALID");
        o.put("motif", "Adresse invalide : incohérence code postal / ville");
        o.put("actionConseiller", "VÉRIFICATION_OBLIGATOIRE");
        o.put("details", "Le code postal ne correspond pas à la ville indiquée ; vérification obligatoire.");
        o.put("confidence", 0.75);

        return o;
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
    
    private String convertTypeEnergie(Contrat.Energie energie) {
        if (energie == null) return "Electricité";
        
        switch (energie) {
            case GAZ:
                return "Gaz";
            case ELECTRICITE:
                return "Electricité";
            case DUAL:
                return "Dual";
            default:
                return "Electricité";
        }
    }
}
