package com.verdissia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeClientRequest {
    private String typeDemande;

    private InformationsPersonnelles informationsPersonnelles;

    private InformationsFourniture informationsFourniture;

    private Boolean consentementClient;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InformationsPersonnelles {
        private String referenceClient;
        private String civilite;
        private String prenom;
        private String nom;
        private String email;
        private String telephone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InformationsFourniture {
        private String voie;
        private String codePostal;
        private String ville;
        private String typeEnergie;
        private String dateMiseEnService;
        private String preferenceOffre;
        private Integer offre;
    }
}