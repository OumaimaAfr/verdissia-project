package com.verdissia.service;

import com.verdissia.dto.request.SignatureConfirmationRequest;
import com.verdissia.dto.response.DemandeResponse;
import com.verdissia.model.Contrat;
import com.verdissia.model.DemandeClient;
import com.verdissia.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContratRepository contratRepository;
    private final DemandeClientService demandeClientService;
    private final TokenService tokenService;

    @Transactional
    public Contrat createContractFromSignature(SignatureConfirmationRequest request) {
        log.info("Creating contract for demande: {} with statut: {}", request.getIdDemande(), request.getStatutSignature());

        // Get demande details
         DemandeResponse demande = demandeClientService.getDemandeById(request.getIdDemande());
        
        // Check if contract already exists for this client
        log.info("Checking if contract exists for client reference: {}", demande.getClientReference());
        if (contratRepository.existsByReferenceClient(demande.getClientReference())) {
            contratRepository.findByReferenceClient(demande.getClientReference())
                .ifPresent(existingContract -> log.warn("Found existing contract: numero={}, email={}", 
                    existingContract.getNumeroContrat()));
            throw new RuntimeException("Un contrat existe déjà pour ce client");
        }

        // Build contract using existing Contrat entity
        Contrat contrat = Contrat.builder()
                .numeroContrat(generateNumeroContrat())
                .referenceClient(demande.getClientReference())
                .civilite(demande.getClientCivilite())
                .nom(demande.getClientNom())
                .prenom(demande.getClientPrenom())
                .email(demande.getClientEmail())
                .telephone(demande.getClientTelephone())
                .typeEnergie(mapToEnergieType(demande.getOffreTypeEnergie()))
                .libelleOffre(demande.getOffreLibelle())
                .voieLivraison(demande.getVoie())
                .codePostalLivraison(demande.getCodePostal())
                .villeLivraison(demande.getVille())
                .prix(BigDecimal.valueOf(demande.getOffrePrix()))
                .dateSignature(LocalDateTime.now())
                .dateMiseEnService(demande.getDateMiseEnService())
                .paiementTraite(false)
                .consentementClient(demande.getConsentementClient())
                .statutLlm(Contrat.StatutLlm.PENDING)
                .build();

        // Save contract
        Contrat savedContrat = contratRepository.save(contrat);

        // Update demande status
        demandeClientService.updateDemandeStatus(request.getIdDemande(), "VALIDEE");

        // Mark token as used if applicable
        markTokenAsUsedForDemande(request.getIdDemande());

        log.info("Contract created successfully with numero: {}", savedContrat.getNumeroContrat());
        return savedContrat;
    }

    private String generateNumeroContrat() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = (int) (Math.random() * 10000);
        return "CTR-" + dateStr + "-" + String.format("%04d", randomNum);
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
        
        return adresse.length() > 0 ? adresse.toString() : "";
    }

    private Contrat.Energie mapToEnergieType(String offreTypeEnergie) {
        if (offreTypeEnergie == null) {
            return Contrat.Energie.ELECTRICITE; // default
        }
        
        switch (offreTypeEnergie.toUpperCase()) {
            case "GAZ":
                return Contrat.Energie.GAZ;
            case "DUAL":
                return Contrat.Energie.DUAL;
            case "ELECTRICITE":
            default:
                return Contrat.Energie.ELECTRICITE;
        }
    }

    private void markTokenAsUsedForDemande(String demandeId) {
        try {
            // Find and mark any active tokens for this demande as used
            tokenService.markTokensAsUsedForDemande(demandeId);
        } catch (Exception e) {
            log.warn("Could not mark token as used for demande {}: {}", demandeId, e.getMessage());
        }
    }
}
