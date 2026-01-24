package com.verdissia.controller;

import com.verdissia.dto.response.IResponseDTO;
import com.verdissia.dto.response.OffreResponse;
import com.verdissia.model.Offre;
import com.verdissia.repository.OffreRepository;
import com.verdissia.service.OffreService;
import com.verdissia.util.ResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@Validated
public class OffreController {

    private final OffreService offreService;

    public OffreController(OffreService offreService, OffreRepository offreRepository) {
        this.offreService = offreService;
    }

    @GetMapping("/search")
    public ResponseEntity<IResponseDTO> search(
            @RequestParam Offre.TypeEnergie typeEnergie,
            @RequestParam String preferenceOffre
    ) {
        List<OffreResponse> result = offreService.search(typeEnergie, preferenceOffre);
        return ResponseHelper.returnSuccess(result);
    }
}
