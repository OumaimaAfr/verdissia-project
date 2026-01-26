package com.verdissia.service;

import com.verdissia.dto.response.OffreResponse;
import com.verdissia.model.Offre;
import com.verdissia.repository.OffreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OffreService {

    private final OffreRepository offreRepository;

    public OffreService(OffreRepository offreRepository) {
        this.offreRepository = offreRepository;
    }

    public List<OffreResponse> search(Offre.TypeEnergie typeEnergie, String preferenceOffre) {
        List<Offre> offres = offreRepository.findByTypeEnergieAndPreferenceOffreIgnoreCase(typeEnergie, preferenceOffre);
        return offres.stream()
                .map(o -> OffreResponse.builder()
                        .id(o.getId())
                        .libelle(o.getLibelle())
                        .description(o.getDescription())
                        .typeEnergie(o.getTypeEnergie() != null ? o.getTypeEnergie().name() : null)
                        .preferenceOffre(o.getPreferenceOffre())
                        .prix(o.getPrix())
                        .build())
                .collect(Collectors.toList());
    }
}
