package com.verdissia.service;

import com.verdissia.dto.ValidationResult;
import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.mapper.DemandeMapper;
import com.verdissia.model.Client;
import com.verdissia.model.DemandeClient;
import com.verdissia.model.Offre;
import com.verdissia.model.SignatureToken;
import com.verdissia.repository.ClientRepository;
import com.verdissia.repository.DemandeClientRepository;
import com.verdissia.repository.OffreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DemandeMapper demandeMapper;
    private final EmailService emailService;
    private final TokenService tokenService;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    private static final DateTimeFormatter DATE_FORMATTER_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER_DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public DemandeResponse getDemandeById(String id) {
        log.info("Récupération de la demande avec l'identifiant: {}", id);

        DemandeClient demande = demandeClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'identifiant: " + id));

        return demandeMapper.toResponse(demande);
    }

    public DemandeResponse createDemande(DemandeClientRequest request) {
        log.info("Création de la demande pour le client: {}", request.getInformationsPersonnelles().getEmail());
        
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
                log.warn("Le client {} a déjà une demande en cours", referenceClient);
                throw new RuntimeException("Vous avez déjà une demande en cours de traitement");
            }

            // Validate required fields
            ValidationResult validation = validateRequiredFields(request);

            if (validation.isValid()) {
                // All fields present - save directly with EN_ATTENTE_SIGNATURE status and send email
                return processCompleteDemande(request);
            } else {
                // Missing fields - return error without saving
                throw new RuntimeException("Champs obligatoires manquants: " + validation.getErrors());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de la demande pour le client: {}", request.getInformationsPersonnelles().getEmail(), e);
            throw new RuntimeException("Échec de la création de la demande: " + e.getMessage(), e);
        }
    }

    private DemandeResponse processCompleteDemande(DemandeClientRequest request) {
        log.info("Traitement de la demande complète pour le client: {}", request.getInformationsPersonnelles().getEmail());

        try {
            // Find or create client
            Client client = findOrCreateClient(request.getInformationsPersonnelles(), request.getInformationsFourniture());

            // Find offre
            Offre offre = offreRepository.findById(request.getInformationsFourniture().getOffre())
                    .orElseThrow(() -> new RuntimeException("Offre non trouvée avec l'identifiant: " + request.getInformationsFourniture().getOffre()));

            // Parse date if provided
            LocalDateTime dateMiseEnService = null;
            if (request.getInformationsFourniture().getDateMiseEnService() != null &&
                    !request.getInformationsFourniture().getDateMiseEnService().isEmpty()) {
                try {
                    dateMiseEnService = java.time.LocalDate.parse(request.getInformationsFourniture().getDateMiseEnService(), DATE_FORMATTER_YMD).atStartOfDay();
                } catch (Exception e1) {
                    try {
                        dateMiseEnService = java.time.LocalDate.parse(request.getInformationsFourniture().getDateMiseEnService(), DATE_FORMATTER_DMY).atStartOfDay();
                    } catch (Exception e2) {
                        throw new RuntimeException("Format de date invalide. Formats acceptés: yyyy-MM-dd ou dd-MM-yyyy. Date reçue: " + request.getInformationsFourniture().getDateMiseEnService());
                    }
                }
            }

            // Create demande with EN_ATTENTE_SIGNATURE status
            DemandeClient demande = DemandeClient.builder()
                    .typeDemande(request.getTypeDemande())
                    .referenceClient(request.getInformationsPersonnelles().getReferenceClient())
                    .client(client)
                    .offre(offre)
                    .consentementClient(request.getConsentementClient())
                    .dateMiseEnService(dateMiseEnService)
                    .statut(DemandeClient.StatutDemande.EN_ATTENTE_SIGNATURE)
                    .build();

            DemandeClient savedDemande = demandeClientRepository.save(demande);
            log.info("Demande créée avec succès avec l'identifiant: {} et statut EN_ATTENTE_SIGNATURE", savedDemande.getId());

            // Generate token and send email
            sendSignatureEmail(savedDemande);

            return demandeMapper.toResponse(savedDemande);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de la demande complète", e);
            throw new RuntimeException("Échec du traitement de la demande complète: " + e.getMessage(), e);
        }
    }

    private void sendSignatureEmail(DemandeClient savedDemande) {
        try {
            String clientEmail = savedDemande.getClient().getEmail();
            String clientName = savedDemande.getClient().getPrenom() + " " + savedDemande.getClient().getNom();

            // Check for existing active token or generate new one
            SignatureToken token = tokenService.findActiveTokenOrGenerateNew(clientEmail, savedDemande);

            // Send email with the token
            emailService.sendSignatureEmail(clientEmail, token.getToken(), clientName);
            log.info("Email de signature envoyé à {} pour la demande {} avec token {}",
                    clientEmail, savedDemande.getId(), token.getToken());

        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de signature pour la demande {}: {}", savedDemande.getId(), e.getMessage(), e);
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

    public DemandeClient getDemandeEntityById(String id) {
        return demandeClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID: " + id));
    }

    public void updateDemandeStatus(String id, String statut) {
        DemandeClient demande = getDemandeEntityById(id);
        try {
            DemandeClient.StatutDemande newStatut = DemandeClient.StatutDemande.valueOf(statut);
            demande.setStatut(newStatut);
            demande.setDateTraitement(LocalDateTime.now());
            demandeClientRepository.save(demande);
            log.info("Updated demande {} status to {}", id, statut);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + statut);
        }
    }
}
