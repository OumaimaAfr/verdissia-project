package com.verdissia.controller;

import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.model.SignatureToken;
import com.verdissia.service.DemandeClientService;
import com.verdissia.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
@Slf4j
public class SignatureController {

    private final TokenService tokenService;
    private final DemandeClientService demandeClientService;

    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateToken(@PathVariable String token) {
        try {
            SignatureToken signatureToken = tokenService.validateToken(token);
            
            if (signatureToken == null) {
                return ResponseEntity.badRequest().body("Token invalide ou expiré");
            }

            // Get demande details
            DemandeResponse demande = demandeClientService.getDemandeById(signatureToken.getDemande().getId());
            
            return ResponseEntity.ok().body(demande);
            
        } catch (Exception e) {
            log.error("Error validating token {}: {}", token, e.getMessage());
            return ResponseEntity.internalServerError().body("Erreur lors de la validation du token");
        }
    }

    @PostMapping("/complete/{token}")
    public ResponseEntity<?> completeSignature(@PathVariable String token) {
        try {
            SignatureToken signatureToken = tokenService.validateToken(token);
            
            if (signatureToken == null) {
                return ResponseEntity.badRequest().body("Token invalide ou expiré");
            }

            // Mark token as used
            tokenService.markTokenAsUsed(token);
            
            // Update demande status (you might want to add this logic to DemandeClientService)
            // demandeClientService.updateDemandeStatus(signatureToken.getDemande().getId(), "VALIDEE");
            
            log.info("Signature completed for token {}", token);
            
            return ResponseEntity.ok().body("Signature complétée avec succès");
            
        } catch (Exception e) {
            log.error("Error completing signature for token {}: {}", token, e.getMessage());
            return ResponseEntity.internalServerError().body("Erreur lors de la finalisation de la signature");
        }
    }
}
