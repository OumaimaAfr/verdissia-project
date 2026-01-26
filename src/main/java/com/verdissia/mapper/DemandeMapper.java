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
                .clientReference(demande.getClient() != null ? demande.getClient().getReferenceClient() : null)
                .clientCivilite(demande.getClient() != null ? demande.getClient().getCivilite() : null)
                .clientNom(demande.getClient() != null ? demande.getClient().getNom() : null)
                .clientPrenom(demande.getClient() != null ? demande.getClient().getPrenom() : null)
                .clientEmail(demande.getClient() != null ? demande.getClient().getEmail() : null)
                .clientTelephone(demande.getClient() != null ? demande.getClient().getTelephone() : null)
                .offreTypeEnergie(demande.getOffre() != null ? demande.getOffre().getTypeEnergie().name() : null)
                .typeDemande(demande.getTypeDemande())
                .statut(demande.getStatut() != null ? demande.getStatut().name() : null)
                .motifRejet(demande.getMotifRejet())
                .offreId(demande.getOffre() != null ? demande.getOffre().getId() : null)
                .offreLibelle(demande.getOffre() != null ? demande.getOffre().getLibelle() : null)
                .offrePrix(demande.getOffre() != null ? demande.getOffre().getPrix().doubleValue() : null)
                .consentementClient(demande.getConsentementClient())
                .dateCreation(demande.getDateCreation())
                .dateTraitement(demande.getDateTraitement())
                .dateMiseEnService(demande.getDateMiseEnService())
                .voie(demande.getClient() != null ? demande.getClient().getVoie() : null)
                .codePostal(demande.getClient() != null ? demande.getClient().getCodePostal() : null)
                .ville(demande.getClient() != null ? demande.getClient().getVille() : null)
                .build();
    }
}
