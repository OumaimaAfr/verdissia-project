package com.verdissia.controller;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.service.DemandeClientService;
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
    public ResponseEntity<DemandeResponse> createDemande(
            @Valid @RequestBody DemandeClientRequest demandeRequest) {
        
        log.info("Reçu une nouvelle demande de type: {}", demandeRequest.getTypeDemande());
        
        try {
            DemandeResponse response = demandeClientService.createDemande(demandeRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Erreur lors de la création de la demande: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création de la demande: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeResponse> getDemandeById(@PathVariable String id) {
        // TODO: Implement getDemandeById in service
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
