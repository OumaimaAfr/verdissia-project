package com.verdissia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratResponse {

    private Long id;
    private String numeroContrat;
    private Long clientId;
    private String clientEmail;
    private String clientTelephone;
    private String demandeId;
    private Integer offreId;
    private String typeEnergie;
    private String offre;
    private String adresseLivraison;
    private String codePostalLivraison;
    private String villeLivraison;
    private BigDecimal prix;
    private String statut;
    private String modePaiement;
    private String iban;
    private LocalDateTime dateCreation;
    private LocalDateTime dateSignature;
    private LocalDateTime dateActivation;
}