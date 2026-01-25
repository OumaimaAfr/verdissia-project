package com.verdissia.service;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.llm.LlmMapper;
import com.verdissia.llm.demande.LlmDecisionEngine;
import com.verdissia.llm.service.LLMValidationService;
import com.verdissia.mapper.DemandeMapper;
import com.verdissia.model.Client;
import com.verdissia.model.DemandeClient;
import com.verdissia.model.Offre;
import com.verdissia.repository.ClientRepository;
import com.verdissia.repository.DemandeClientRepository;
import com.verdissia.repository.OffreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DemandeClientService {

    private final DemandeClientRepository demandeClientRepository;
    private final ClientRepository clientRepository;
    private final OffreRepository offreRepository;
    private final LLMValidationService llmValidationService;
    private final LlmDecisionEngine llmDecisionEngine;
    private final LlmMapper llmMapper;
    private final ObjectMapper objectMapper;

    private final DemandeMapper demandeMapper; // Continue d'ici

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DemandeResponse createDemande(DemandeClientRequest request) {
        log.info("Creating demande for client: {}", request.getInformationsPersonnelles().getEmail());
        
        try {
            // Find or create client
            Client client = findOrCreateClient(request.getInformationsPersonnelles(), request.getInformationsFourniture());
            
            // Find offre
            Offre offre = offreRepository.findById(request.getInformationsFourniture().getOffre())
                    .orElseThrow(() -> new RuntimeException("Offre not found with id: " + request.getInformationsFourniture().getOffre()));

            // Create demande with EN_ATTENTE status
            DemandeClient demande = DemandeClient.builder()
                    .typeDemande(request.getTypeDemande())
                    .client(client)
                    .offre(offre)
                    .consentementClient(request.getConsentementClient())
                    .statut(DemandeClient.StatutDemande.EN_ATTENTE)
                    .build();
            
            DemandeClient savedDemande = demandeClientRepository.save(demande);
            log.info("Demande created successfully with id: {} and status EN_ATTENTE", savedDemande.getId());
            
            // Process LLM validation
            return processLLMValidation(savedDemande, request, offre);
            
        } catch (Exception e) {
            log.error("Error creating demande for client: {}", request.getInformationsPersonnelles().getEmail(), e);
            throw new RuntimeException("Failed to create demande: " + e.getMessage(), e);
        }
    }
    
    private DemandeResponse processLLMValidation(DemandeClient demande, DemandeClientRequest request, Offre offre) {
        log.info("Starting LLM validation for demande: {}", demande.getId());
        
        try {
            // Update status to EN_COURS during LLM processing
            demande.setStatut(DemandeClient.StatutDemande.EN_COURS);
            demandeClientRepository.save(demande);
            log.info("Demande {} status updated to EN_COURS", demande.getId());
            
            // Convert request to LLM input format
            var llmInput = llmMapper.toInput(request, offre);
            
            // Validate with LLM service
            var validationResult = llmValidationService.validate(llmInput);
            log.info("LLM validation completed for demande: {} with confidence: {}", demande.getId(), validationResult.confidence());
            
            // Apply decision based on threshold
            var decisionResult = llmDecisionEngine.decide(validationResult);
            
            // Update demande with final decision
            switch (decisionResult) {
                case "OK" -> {
                    demande.setStatut(DemandeClient.StatutDemande.VALIDEE);
                    demande.setMotifRejet(null);
                }
                case "KO" -> {
                    demande.setStatut(DemandeClient.StatutDemande.REJETEE);
                    // Build rejection reason from validation errors
                    String motifRejet = buildMotifRejet(validationResult);
                    demande.setMotifRejet(motifRejet);
                }
                case "REVIEW" -> {
                    demande.setStatut(DemandeClient.StatutDemande.EN_COURS);
                    demande.setMotifRejet("Demande nécessite une révision manuelle");
                }
                default -> {
                    demande.setStatut(DemandeClient.StatutDemande.EN_ERREUR);
                    demande.setMotifRejet("Décision inconnue: " + decisionResult);
                }
            }
            
            demande.setDateTraitement(LocalDateTime.now());
            DemandeClient updatedDemande = demandeClientRepository.save(demande);
            log.info("Demande {} processed with final status: {}", updatedDemande.getId(), updatedDemande.getStatut());
            
            return demandeMapper.toResponse(updatedDemande);
            
        } catch (Exception e) {
            log.error("LLM processing failed for demande: {}", demande.getId(), e);
            
            // Update status to EN_ERREUR on technical failure
            try {
                demande.setStatut(DemandeClient.StatutDemande.EN_ERREUR);
                demande.setDateTraitement(LocalDateTime.now());
                demande.setMotifRejet("Erreur technique lors du traitement: " + e.getMessage());
                demandeClientRepository.save(demande);
            } catch (Exception saveException) {
                log.error("Failed to update error status for demande: {}", demande.getId(), saveException);
            }
            
            throw new RuntimeException("LLM processing failed: " + e.getMessage(), e);
        }
    }
    
    private Client findOrCreateClient(DemandeClientRequest.InformationsPersonnelles infos, DemandeClientRequest.InformationsFourniture fourniture) {
        return clientRepository.findByEmail(infos.getEmail())
                .orElseGet(() -> createNewClient(infos, fourniture));
    }
    
    private Client createNewClient(DemandeClientRequest.InformationsPersonnelles infos, DemandeClientRequest.InformationsFourniture fourniture) {
        Client client = Client.builder()
                .referenceClient(infos.getReferenceClient())
                .civilite(infos.getCivilite())
                .prenom(infos.getPrenom())
                .nom(infos.getNom())
                .email(infos.getEmail())
                .telephone(infos.getTelephone())
                .voie(fourniture.getVoie())
                .codePostal(fourniture.getCodePostal())
                .ville(fourniture.getVille())
                .build();
        
        return clientRepository.save(client);
    }
    
    private String buildMotifRejet(com.verdissia.llm.dto.LLMDemandeResult validationResult) {
        if (validationResult.errors() != null && !validationResult.errors().isEmpty()) {
            StringBuilder motif = new StringBuilder("Erreurs de validation: ");
            validationResult.errors().forEach(error -> 
                motif.append(error.field()).append(" - ").append(error.message()).append("; ")
            );
            return motif.toString();
        }
        
        if (validationResult.notes() != null && !validationResult.notes().trim().isEmpty()) {
            return "Rejet: " + validationResult.notes();
        }
        
        return "Demande rejetée suite à validation LLM";
    }
}
