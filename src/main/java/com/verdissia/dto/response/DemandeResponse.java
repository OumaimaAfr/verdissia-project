package com.verdissia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeResponse {
    private String id;
    private Long clientId;
    private String clientReference;
    private String clientCivilite;
    private String clientTelephone;
    private String offreTypeEnergie;
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    private String typeDemande;
    private String statut;
    private String motifRejet;
    private Long contratId;
    private Integer offreId;
    private String offreLibelle;
    private Double offrePrix;
    private Boolean consentementClient;
    private LocalDateTime dateCreation;
    private LocalDateTime dateTraitement;
    private LocalDateTime dateMiseEnService;
    private String voie;
    private String codePostal;
    private String ville;
}
