package com.verdissia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String actionConseiller;
    
    private BigDecimal confidence;
    
    private String decision;
    
    private String details;
    
    private String motifCode;
    
    private String messageErreur;
    
    private String motifMessage;
    
    private String civilite;
    
    private String nom;
    
    private String prenom;
    
    private String voie;
    
    private String codePostal;
    
    private String ville;
    
    private Boolean consentementClient;
    
    private String email;
    
    private String telephone;
    
    private String typeEnergie;
    
    private String offre;
    
    private String dateMiseEnService;
    
    private String libelleOffre;
    
    private BigDecimal prix;
    
    private Boolean paiementTraite;
    
    private String numeroContrat;
}
