package com.verdissia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreResponse {

    private Integer id;
    private String libelle;
    private String description;
    private String energie;
    private BigDecimal prixBase;
    private BigDecimal prixAbonnementMensuel;
    private Boolean offreVerte;
}