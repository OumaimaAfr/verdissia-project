package com.verdissia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratRequest {
    private Long clientId;
    private String demandeId;
    private Integer offreId;
    private String typeEnergie;
    private String adresseLivraison;
    private String codePostalLivraison;
    private String villeLivraison;
    private BigDecimal prix;
    private String modePaiement;
    private String iban;
}