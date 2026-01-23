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
public class OffreResponse {

    private Integer id;
    private String libelle;
    private String description;
    private String typeEnergie;
    private String preferenceOffre;
    private BigDecimal prix;
    private LocalDateTime dateMiseEnService;
}