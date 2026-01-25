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
import java.util.List;

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
            // Check if client already has a pending demande
            String referenceClient = request.getInformationsPersonnelles().getReferenceClient();
            List<DemandeClient> allClientDemandes = demandeClientRepository.findByReferenceClient(referenceClient);
            
            // Filter for pending statuses in Java
            boolean hasPendingDemande = allClientDemandes.stream()
                    .anyMatch(d -> d.getStatut() == DemandeClient.StatutDemande.EN_ATTENTE ||
                                  d.getStatut() == DemandeClient.StatutDemande.EN_ATTENTE_SIGNATURE ||
                                  d.getStatut() == DemandeClient.StatutDemande.EN_COURS);
            
            if (hasPendingDemande) {
                log.warn("Client {} already has a pending demande", referenceClient);
                throw new RuntimeException("Vous avez déjà une demande en cours de traitement");
            }
            
            // Validate required fields
            ValidationResult validation = validateRequiredFields(request);
            
            if (validation.isValid()) {
                // All fields present - save directly with EN_ATTENTE_SIGNATURE status
                return processCompleteDemande(request);
            } else {
                // Missing fields - technical error
                return processIncompleteDemande(request, validation.getErrors());
            }
            
        } catch (RuntimeException e) {
            // Re-throw business exceptions (like pending demande)
            throw e;
        } catch (Exception e) {
            log.error("Error creating demande for client: {}", request.getInformationsPersonnelles().getEmail(), e);
            throw new RuntimeException("Failed to create demande: " + e.getMessage(), e);
        }
    }
    
    private DemandeResponse processCompleteDemande(DemandeClientRequest request) {
        log.info("Processing complete demande for client: {}", request.getInformationsPersonnelles().getEmail());
        
        try {
            // Find or create client
            Client client = findOrCreateClient(request.getInformationsPersonnelles(), request.getInformationsFourniture());
            
            // Find offre
            Offre offre = offreRepository.findById(request.getInformationsFourniture().getOffre())
                    .orElseThrow(() -> new RuntimeException("Offre not found with id: " + request.getInformationsFourniture().getOffre()));

            // Create demande with EN_ATTENTE_SIGNATURE status
            DemandeClient demande = DemandeClient.builder()
                    .typeDemande(request.getTypeDemande())
                    .referenceClient(request.getInformationsPersonnelles().getReferenceClient())
                    .client(client)
                    .offre(offre)
                    .consentementClient(request.getConsentementClient())
                    .statut(DemandeClient.StatutDemande.EN_ATTENTE_SIGNATURE)
                    .build();
            
            DemandeClient savedDemande = demandeClientRepository.save(demande);
            log.info("Demande created successfully with id: {} and status EN_ATTENTE_SIGNATURE", savedDemande.getId());
            
            return demandeMapper.toResponse(savedDemande);
            
        } catch (Exception e) {
            log.error("Error processing complete demande", e);
            throw new RuntimeException("Failed to process complete demande: " + e.getMessage(), e);
        }
    }
    
    private DemandeResponse processIncompleteDemande(DemandeClientRequest request, String errors) {
        log.info("Processing incomplete demande for client: {} with errors: {}", 
                request.getInformationsPersonnelles().getEmail(), errors);
        
        try {
            // Find or create client
            Client client = findOrCreateClient(request.getInformationsPersonnelles(), request.getInformationsFourniture());
            
            // Find offre
            Offre offre = offreRepository.findById(request.getInformationsFourniture().getOffre())
                    .orElseThrow(() -> new RuntimeException("Offre not found with id: " + request.getInformationsFourniture().getOffre()));

            // Create demande with EN_ERREUR status
            DemandeClient demande = DemandeClient.builder()
                    .typeDemande(request.getTypeDemande())
                    .referenceClient(request.getInformationsPersonnelles().getReferenceClient())
                    .client(client)
                    .offre(offre)
                    .consentementClient(request.getConsentementClient())
                    .statut(DemandeClient.StatutDemande.EN_ERREUR)
                    .motifRejet("Erreur technique: champs obligatoires manquants - " + errors)
                    .dateTraitement(LocalDateTime.now())
                    .build();
            
            DemandeClient savedDemande = demandeClientRepository.save(demande);
            log.info("Demande created with error status for client: {} - {}", 
                    savedDemande.getId(), savedDemande.getMotifRejet());
            
            return demandeMapper.toResponse(savedDemande);
            
        } catch (Exception e) {
            log.error("Error processing incomplete demande", e);
            throw new RuntimeException("Failed to process incomplete demande: " + e.getMessage(), e);
        }
    }
    
    private ValidationResult validateRequiredFields(DemandeClientRequest request) {
        StringBuilder errors = new StringBuilder();
        
        // Validate informations personnelles
        if (request.getInformationsPersonnelles() == null) {
            errors.append("Informations personnelles manquantes; ");
        } else {
            var infos = request.getInformationsPersonnelles();
            if (infos.getReferenceClient() == null || infos.getReferenceClient().trim().isEmpty()) {
                errors.append("Référence client manquante; ");
            }
            if (infos.getCivilite() == null || infos.getCivilite().trim().isEmpty()) {
                errors.append("Civilité manquante; ");
            }
            if (infos.getPrenom() == null || infos.getPrenom().trim().isEmpty()) {
                errors.append("Prénom manquant; ");
            }
            if (infos.getNom() == null || infos.getNom().trim().isEmpty()) {
                errors.append("Nom manquant; ");
            }
            if (infos.getEmail() == null || infos.getEmail().trim().isEmpty()) {
                errors.append("Email manquant; ");
            }
            if (infos.getTelephone() == null || infos.getTelephone().trim().isEmpty()) {
                errors.append("Téléphone manquant; ");
            }
        }
        
        // Validate informations fourniture
        if (request.getInformationsFourniture() == null) {
            errors.append("Informations de fourniture manquantes; ");
        } else {
            var fourniture = request.getInformationsFourniture();
            if (fourniture.getVoie() == null || fourniture.getVoie().trim().isEmpty()) {
                errors.append("Voie manquante; ");
            }
            if (fourniture.getCodePostal() == null || fourniture.getCodePostal().trim().isEmpty()) {
                errors.append("Code postal manquant; ");
            }
            if (fourniture.getVille() == null || fourniture.getVille().trim().isEmpty()) {
                errors.append("Ville manquante; ");
            }
            if (fourniture.getTypeEnergie() == null || fourniture.getTypeEnergie().trim().isEmpty()) {
                errors.append("Type d'énergie manquant; ");
            }
            if (fourniture.getDateMiseEnService() == null || fourniture.getDateMiseEnService().trim().isEmpty()) {
                errors.append("Date de mise en service manquante; ");
            }
            if (fourniture.getOffre() == null) {
                errors.append("Offre manquante; ");
            }
        }
        
        // Validate other required fields
        if (request.getTypeDemande() == null || request.getTypeDemande().trim().isEmpty()) {
            errors.append("Type de demande manquant; ");
        }
        if (request.getConsentementClient() == null) {
            errors.append("Consentement client manquant; ");
        }
        
        boolean isValid = errors.length() == 0;
        return new ValidationResult(isValid, isValid ? "" : errors.toString());
    }
    
    private static class ValidationResult {
        private final boolean valid;
        private final String errors;
        
        public ValidationResult(boolean valid, String errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrors() {
            return errors;
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
}
