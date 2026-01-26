package com.verdissia.llm.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionConseiller {
    
    private String type; // APPELER_CLIENT, REJETER_DEMANDE, CONTACTER_CLIENT
    
    private String motif; // Pourquoi cette action est recommandée
    
    private String instructions; // Instructions détaillées pour le conseiller
    
    private String priorite; // HAUTE, MOYENNE, BASSE
}
