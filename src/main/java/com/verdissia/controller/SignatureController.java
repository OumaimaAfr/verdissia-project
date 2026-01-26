package com.verdissia.controller;

import com.verdissia.dto.TokenValidationResult;
import com.verdissia.dto.request.SignatureConfirmationRequest;
import com.verdissia.dto.request.TokenRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.dto.response.IResponseDTO;
import com.verdissia.dto.response.SignatureInfoResponse;
import com.verdissia.dto.response.SimpleStatusResponse;
import com.verdissia.model.Contrat;
import com.verdissia.model.SignatureToken;
import com.verdissia.service.ContractService;
import com.verdissia.service.DemandeClientService;
import com.verdissia.service.TokenService;
import com.verdissia.util.ResponseHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/verdisia")
@RequiredArgsConstructor
@Slf4j
public class SignatureController {

    private final TokenService tokenService;
    private final DemandeClientService demandeClientService;
    private final ContractService contractService;

    @PostMapping("/signature/init")
    public ResponseEntity<?> validateToken(@Valid @RequestBody TokenRequest tokenRequest) {
        try {
            TokenValidationResult validationResult = tokenService.validateTokenWithDetails(tokenRequest.getToken());
            
            if (!validationResult.isValid()) {
                return ResponseHelper.returnError(HttpStatus.BAD_REQUEST, "TOKEN_INVALID", validationResult.getMessage());
            }

            // Get demande details
            DemandeResponse demande = demandeClientService.getDemandeById(validationResult.getToken().getDemande().getId());
            
            // Build signature info response
            String adresse = buildAdresse(demande.getVoie(), demande.getCodePostal(), demande.getVille());
            String dateMiseEnServiceStr = formatDateMiseEnService(demande.getDateMiseEnService());
            
            SignatureInfoResponse response = SignatureInfoResponse.builder()
                    .nom(demande.getClientNom())
                    .prenom(demande.getClientPrenom())
                    .offre(SignatureInfoResponse.OffreInfo.builder()
                            .libelle(demande.getOffreLibelle())
                            .prix(demande.getOffrePrix())
                            .build())
                    .dateMiseEnService(dateMiseEnServiceStr)
                    .adresse(adresse)
                    .idDemande(demande.getId())
                    .build();
            
            return ResponseHelper.returnSuccess(response);
            
        } catch (Exception e) {
            log.error("Error validating token {}: {}", tokenRequest.getToken(), e.getMessage());
            return ResponseHelper.returnError(HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICAL_ERROR", 
                "Erreur technique lors de la validation du token. Veuillez réessayer ou nous contacter.");
        }
    }
    
    private String buildAdresse(String voie, String codePostal, String ville) {
        StringBuilder adresse = new StringBuilder();
        
        if (voie != null && !voie.trim().isEmpty()) {
            adresse.append(voie.trim());
        }
        
        if (codePostal != null && !codePostal.trim().isEmpty()) {
            if (adresse.length() > 0) {
                adresse.append(", ");
            }
            adresse.append(codePostal.trim());
        }
        
        if (ville != null && !ville.trim().isEmpty()) {
            if (adresse.length() > 0) {
                adresse.append(" ");
            }
            adresse.append(ville.trim());
        }
        
        return adresse.length() > 0 ? adresse.toString() : null;
    }
    
    private String formatDateMiseEnService(LocalDateTime dateMiseEnService) {
        if (dateMiseEnService == null) {
            return null;
        }
        return dateMiseEnService.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @PostMapping("/signature/confirm")
    public ResponseEntity<?> confirmSignature(@Valid @RequestBody SignatureConfirmationRequest request) {
        try {
            log.info("Received signature confirmation for demande: {} with statut: {}", 
                    request.getIdDemande(), request.getStatutSignature());

            // Validate statutSignature
            if (!"SIGNE".equals(request.getStatutSignature())) {
                return ResponseHelper.returnError(HttpStatus.BAD_REQUEST, "INVALID_STATUT", 
                    "Le statut de signature doit être 'SIGNE'");
            }

            // Create contract from signature
            Contrat contrat = contractService.createContractFromSignature(request);

            log.info("Contract created successfully with numero: {}", contrat.getNumeroContrat());
            
            return ResponseHelper.returnSuccess();
            
        } catch (RuntimeException e) {
            log.error("Error confirming signature for demande {}: {}", 
                    request.getIdDemande(), e.getMessage());
            
            // Handle specific business exceptions
            if (e.getMessage().contains("Un contrat existe déjà")) {
                return ResponseHelper.returnError(HttpStatus.CONFLICT, "CONTRACT_EXISTS", e.getMessage());
            }
            
            if (e.getMessage().contains("Demande non trouvée")) {
                return ResponseHelper.returnError(HttpStatus.NOT_FOUND, "DEMANDE_NOT_FOUND", e.getMessage());
            }
            
            // Handle other runtime errors (400)
            return ResponseHelper.returnError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
            
        } catch (Exception e) {
            log.error("Unexpected error confirming signature for demande {}: {}", 
                    request.getIdDemande(), e.getMessage());
            return ResponseHelper.returnError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", 
                "Erreur technique lors de la confirmation de signature. Veuillez réessayer ou nous contacter.");
        }
    }
}
