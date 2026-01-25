package com.verdissia.llm.dto;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.model.DemandeClient;
import com.verdissia.model.Offre;

import java.math.BigDecimal;

public record LLMDemandeInput(String typeDemande, DemandeClientRequest.InformationsPersonnelles personalInfo,
                              DemandeClientRequest.InformationsFourniture supplyInfo, boolean consentementClient) {
    public record InformationsPersonnelles(
            String refClient,
            String civilite,
            String nom,
            String prenom,
            String email,
            String telephone
    ) {
    }

    public record InformationsFourniture(
            String voie,
            String codePostal,
            String ville,
            String typeEnergie,
            String dateMiseService,
            String preferenceOffre,
            Integer offreId
    ) {
    }

}
