package com.verdissia.controller;

import com.verdissia.service.ContratLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verdisia/llm")
@RequiredArgsConstructor
@Slf4j
public class LlmController {

    private final ContratLlmService contratLlmService;

    @PostMapping("/process-pending")
    public ResponseEntity<String> processPendingContrats() {
        log.info("Déclenchement manuel du traitement des contrats en attente");
        
        try {
            contratLlmService.processPendingContrats();
            return ResponseEntity.ok("Traitement des contrats en attente déclenché avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du déclenchement du traitement", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors du traitement: " + e.getMessage());
        }
    }
}
