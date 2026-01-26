package com.verdissia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureInfoResponse {
    private String nom;
    private String prenom;
    private OffreInfo offre;
    private String dateMiseEnService;
    private String adresse;
    private String idDemande;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffreInfo {
        private String libelle;
        private Double prix;
    }
}
