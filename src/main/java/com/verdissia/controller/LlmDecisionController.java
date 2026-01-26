package com.verdissia.controller;

import com.verdissia.model.LlmDecision;
import com.verdissia.repository.LlmDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/verdisia/llm-decisions")
@RequiredArgsConstructor
@Slf4j
public class LlmDecisionController {

    private final LlmDecisionRepository llmDecisionRepository;

    @GetMapping("/contrat/{contratId}")
    public ResponseEntity<List<LlmDecision>> getDecisionsByContratId(@PathVariable Long contratId) {
        log.info("Récupération des décisions LLM pour le contrat: {}", contratId);
        
        List<LlmDecision> decisions = llmDecisionRepository.findByContratId(contratId);
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/latest/{contratId}")
    public ResponseEntity<LlmDecision> getLatestDecisionByContratId(@PathVariable Long contratId) {
        log.info("Récupération de la dernière décision LLM pour le contrat: {}", contratId);
        
        Optional<LlmDecision> decision = llmDecisionRepository.findTopByContratIdOrderByProcessedAtDesc(contratId);
        return decision.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/decision/{decision}")
    public ResponseEntity<List<LlmDecision>> getDecisionsByDecision(@PathVariable String decision) {
        log.info("Récupération des décisions LLM avec décision: {}", decision);
        
        List<LlmDecision> decisions = llmDecisionRepository.findByDecision(decision);
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LlmDecision>> getDecisionsByStatus(@PathVariable String status) {
        log.info("Récupération des décisions LLM avec statut: {}", status);
        
        try {
            LlmDecision.ProcessStatus processStatus = LlmDecision.ProcessStatus.valueOf(status.toUpperCase());
            List<LlmDecision> decisions = llmDecisionRepository.findByProcessStatus(processStatus);
            return ResponseEntity.ok(decisions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<LlmDecision>> getAllDecisions() {
        log.info("Récupération de toutes les décisions LLM");
        
        List<LlmDecision> decisions = llmDecisionRepository.findAll();
        return ResponseEntity.ok(decisions);
    }
}
