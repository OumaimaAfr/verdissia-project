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
    private String clientEmail;
    private String typeDemande;
    private String statut;
    private String motifRejet;
    private Long contratId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateTraitement;
}