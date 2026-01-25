package com.verdissia.mapper;

import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.model.DemandeClient;
import org.springframework.stereotype.Component;

@Component
public class DemandeMapper {

    public DemandeResponse toResponse(DemandeClient demande) {
        return DemandeResponse.builder()
                .id(demande.getId())
                .clientId(demande.getClient() != null ? demande.getClient().getId() : null)
                .clientNom(demande.getClient() != null ? demande.getClient().getNom() : null)
                .clientPrenom(demande.getClient() != null ? demande.getClient().getPrenom() : null)
                .clientEmail(demande.getClient() != null ? demande.getClient().getEmail() : null)
                .typeDemande(demande.getTypeDemande())
                .statut(demande.getStatut() != null ? demande.getStatut().name() : null)
                .motifRejet(demande.getMotifRejet())
                .contratId(demande.getContrat() != null ? demande.getContrat().getId() : null)
                .offreId(demande.getOffre() != null ? demande.getOffre().getId() : null)
                .offreLibelle(demande.getOffre() != null ? demande.getOffre().getLibelle() : null)
                .consentementClient(demande.getConsentementClient())
                .dateCreation(demande.getDateCreation())
                .dateTraitement(demande.getDateTraitement())
                .build();
    }
}
