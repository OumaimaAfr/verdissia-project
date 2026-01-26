package com.verdissia.controller;

import com.verdissia.dto.AuthResponse;
import com.verdissia.llm.mock.LlmAnalysisResult;
import com.verdissia.llm.mock.MockLlmAnalyzer;
import com.verdissia.model.Contrat;
import com.verdissia.model.LlmDecision;
import com.verdissia.repository.LlmDecisionRepository;
import com.verdissia.service.ContratLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/verdisia/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthBOLLMController {

    private final MockLlmAnalyzer mockLlmAnalyzer;
    private final ContratLlmService contratLlmService;
    private final LlmDecisionRepository llmDecisionRepository;

    @PostMapping("/login")
    public ResponseEntity<List<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
        log.info("Connexion du back office pour l'utilisateur: {}", loginRequest.getUsername());
        
        try {
            // TODO: Implémenter la validation réelle du username/password ici
            // Pour l'instant, on accepte n'importe quel username/password
            if (!isValidCredentials(loginRequest.getUsername(), loginRequest.getPassword())) {
                log.warn("Identifiants invalides pour l'utilisateur: {}", loginRequest.getUsername());
                return ResponseEntity.status(401).build();
            }
            // Récupérer tous les contrats
            List<Contrat> allContrats = contratLlmService.getAllContrats();
            
            // Récupérer toutes les décisions LLM
            List<LlmDecision> allDecisions = llmDecisionRepository.findAll();
            
            // Créer un map des décisions les plus récentes par contratId
            Map<Long, LlmDecision> decisionsByContratId = allDecisions.stream()
                    .collect(Collectors.toMap(
                            LlmDecision::getContratId,
                            decision -> decision,
                            (existing, replacement) -> {
                                // Garder la décision la plus récente
                                return existing.getProcessedAt().isAfter(replacement.getProcessedAt()) ? existing : replacement;
                            }
                    ));
            
            List<AuthResponse> responses = new ArrayList<>();
            
            // Pour chaque contrat, créer une réponse complète
            for (Contrat contrat : allContrats) {
                LlmDecision decision = decisionsByContratId.get(contrat.getId());
                
                // Si pas de décision LLM, on peut en créer une à la volée ou laisser null
                if (decision == null) {
                    // Optionnel: analyser le contrat maintenant
                    LlmAnalysisResult analysisResult = mockLlmAnalyzer.analyzeContrat(contrat);
                    
                    AuthResponse response = AuthResponse.builder()
                            .actionConseiller(analysisResult.getActionConseiller())
                            .confidence(analysisResult.getConfidence())
                            .decision(analysisResult.getDecision())
                            .details(analysisResult.getDetails())
                            .motifCode(analysisResult.getMotifCode())
                            .messageErreur(analysisResult.getDecision().equals("REJET") ? analysisResult.getMotif() : null)
                            .motifMessage(analysisResult.getMotif())
                            .civilite(contrat.getCivilite())
                            .nom(contrat.getNom())
                            .prenom(contrat.getPrenom())
                            .voie(contrat.getVoieLivraison())
                            .codePostal(contrat.getCodePostalLivraison())
                            .ville(contrat.getVilleLivraison())
                            .consentementClient(contrat.getConsentementClient())
                            .email(contrat.getEmail())
                            .telephone(contrat.getTelephone())
                            .typeEnergie(contrat.getTypeEnergie() != null ? contrat.getTypeEnergie().toString() : null)
                            .offre(contrat.getLibelleOffre())
                            .dateMiseEnService(contrat.getDateMiseEnService() != null ? 
                                    contrat.getDateMiseEnService().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                            .libelleOffre(contrat.getLibelleOffre())
                            .prix(contrat.getPrix())
                            .paiementTraite(contrat.getPaiementTraite())
                            .numeroContrat(contrat.getNumeroContrat())
                            .build();
                    
                    responses.add(response);
                } else {
                    // Utiliser la décision existante
                    AuthResponse response = AuthResponse.builder()
                            .actionConseiller(decision.getActionConseiller())
                            .confidence(decision.getConfidence())
                            .decision(decision.getDecision())
                            .details(decision.getDetails())
                            .motifCode(decision.getMotifCode())
                            .messageErreur(decision.getDecision().equals("REJET") ? decision.getMotifMessage() : null)
                            .motifMessage(decision.getMotifMessage())
                            .civilite(contrat.getCivilite())
                            .nom(contrat.getNom())
                            .prenom(contrat.getPrenom())
                            .voie(contrat.getVoieLivraison())
                            .codePostal(contrat.getCodePostalLivraison())
                            .ville(contrat.getVilleLivraison())
                            .consentementClient(contrat.getConsentementClient())
                            .email(contrat.getEmail())
                            .telephone(contrat.getTelephone())
                            .typeEnergie(contrat.getTypeEnergie() != null ? contrat.getTypeEnergie().toString() : null)
                            .offre(contrat.getLibelleOffre())
                            .dateMiseEnService(contrat.getDateMiseEnService() != null ? 
                                    contrat.getDateMiseEnService().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                            .libelleOffre(contrat.getLibelleOffre())
                            .prix(contrat.getPrix())
                            .paiementTraite(contrat.getPaiementTraite())
                            .numeroContrat(contrat.getNumeroContrat())
                            .build();
                    
                    responses.add(response);
                }
            }
            
            log.info("Connexion BO réussie - {} contrats retournés", responses.size());
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Erreur lors de la connexion BO pour l'utilisateur: {}", loginRequest.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private boolean isValidCredentials(String username, String password) {
        // Validation simple pour le hackathon
        // En production, utiliser un vrai système d'authentification
        return username != null && !username.trim().isEmpty() && 
               password != null && !password.trim().isEmpty();
    }
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
