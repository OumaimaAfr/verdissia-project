package com.verdissia.controller;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.dto.response.IResponseDTO;
import com.verdissia.service.DemandeClientService;
import com.verdissia.util.ResponseHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verdisia")
@RequiredArgsConstructor
@Slf4j
public class DemandeClientController {

    private final DemandeClientService demandeClientService;

    @PostMapping("/soumission")
    public ResponseEntity<IResponseDTO> createDemande(
            @Valid @RequestBody DemandeClientRequest demandeRequest) {
        
        log.info("Reçu une nouvelle demande de type: {}", demandeRequest.getTypeDemande());
        
        // Vérifier le consentement du client
        if (!Boolean.TRUE.equals(demandeRequest.getConsentementClient())) {
            return ResponseHelper.returnError(HttpStatus.BAD_REQUEST, "CONSENT_REQUIRED",
                "Le consentement du client est obligatoire pour soumettre une demande");
        }

        try {
            demandeClientService.createDemande(demandeRequest);
            return ResponseHelper.returnSuccessOnly(HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Erreur lors de la création de la demande: {}", e.getMessage());
            
            // Handle specific business exceptions
            if (e.getMessage().contains("Vous avez déjà une demande en cours de traitement")) {
                return ResponseHelper.returnError(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
            }

            if (e.getMessage().contains("Offre non trouvée")) {
                return ResponseHelper.returnError(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
            }
            
            // Handle other runtime errors (400)
            return ResponseHelper.returnError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la demande: {}", e.getMessage());
            return ResponseHelper.returnError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        }
    }
}
