package com.verdissia.llm;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.llm.dto.LLMDemandeInput;
import com.verdissia.model.Offre;
import org.springframework.stereotype.Component;

@Component
public class LlmMapper {

    public LLMDemandeInput toInput(DemandeClientRequest req, Offre offre) {

        var p = req.getInformationsPersonnelles();
        var f = req.getInformationsFourniture();

        return new LLMDemandeInput(
                req.getTypeDemande(),
                new DemandeClientRequest.InformationsPersonnelles(
                        p.getReferenceClient(),
                        p.getCivilite(),
                        p.getPrenom(),
                        p.getNom(),
                        p.getEmail(),
                        p.getTelephone()
                ),
                new DemandeClientRequest.InformationsFourniture(
                        f.getVoie(),
                        f.getCodePostal(),
                        f.getVille(),
                        f.getTypeEnergie(),
                        f.getDateMiseEnService(),
                        f.getPreferenceOffre(),
                        f.getOffre()
                ),
                req.getConsentementClient()
        );
    }
}