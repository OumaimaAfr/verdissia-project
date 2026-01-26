package com.verdissia.llm.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmAnalysisResult {
    
    private String decision; // REJET, VALIDE
    
    private String motifCode; // CONSENTMENT_FALSE, EMAIL_INVALID, PHONE_INVALID, etc.
    
    private String motif; // Motif détaillé du rejet/validation
    
    private String actionConseiller; // EXAMINER, TRAITER
    
    private String details; // Détails complémentaires pour le conseiller
    
    private BigDecimal confidence; // Score de confiance 0.00-1.00
    
    private String recommandation; // Action recommandée (appeler client, rejeter, etc.)
}
