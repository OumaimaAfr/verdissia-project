package com.verdissia.service;

import com.verdissia.dto.request.DemandeClientRequest;
import com.verdissia.dto.response.DemandeResponse;
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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DemandeResponse createDemande(DemandeClientRequest request) {
        log.info("Creating demande for client: {}", request.getInformationsPersonnelles().getEmail());
        
        // Find or create client
        Client client = findOrCreateClient(request.getInformationsPersonnelles());
        
        // Find offre
        Offre offre = offreRepository.findById(request.getInformationsFourniture().getOffre())
                .orElseThrow(() -> new RuntimeException("Offre not found with id: " + request.getInformationsFourniture().getOffre()));
        
        // Parse date if provided
        LocalDateTime dateMiseEnService = null;
        if (request.getInformationsFourniture().getDateMiseEnService() != null && 
            !request.getInformationsFourniture().getDateMiseEnService().isEmpty()) {
            dateMiseEnService = LocalDateTime.parse(request.getInformationsFourniture().getDateMiseEnService(), DATE_FORMATTER);
        }
        
        // Create demande
        DemandeClient demande = DemandeClient.builder()
                .typeDemande(request.getTypeDemande())
                .client(client)
                .offre(offre)
                .consentementClient(request.getConsentementClient())
                .build();
        
        DemandeClient savedDemande = demandeClientRepository.save(demande);
        
        log.info("Demande created successfully with id: {}", savedDemande.getId());
        
        return convertToResponse(savedDemande);
    }
    
    private Client findOrCreateClient(DemandeClientRequest.InformationsPersonnelles infos) {
        return clientRepository.findByEmail(infos.getEmail())
                .orElseGet(() -> createNewClient(infos));
    }
    
    private Client createNewClient(DemandeClientRequest.InformationsPersonnelles infos) {
        Client client = Client.builder()
                .referenceClient(infos.getReferenceClient())
                .civilite(infos.getCivilite())
                .prenom(infos.getPrenom())
                .nom(infos.getNom())
                .email(infos.getEmail())
                .telephone(infos.getTelephone())
                .build();
        
        return clientRepository.save(client);
    }
    
    private DemandeResponse convertToResponse(DemandeClient demande) {
        return DemandeResponse.builder()
                .id(demande.getId())
                .typeDemande(demande.getTypeDemande())
                .statut(demande.getStatut().toString())
                .dateCreation(demande.getDateCreation())
                .dateTraitement(demande.getDateTraitement())
                .motifRejet(demande.getMotifRejet())
                .clientId(demande.getClient().getId())
                .clientNom(demande.getClient().getNom())
                .clientPrenom(demande.getClient().getPrenom())
                .clientEmail(demande.getClient().getEmail())
                .offreId(demande.getOffre().getId())
                .offreLibelle(demande.getOffre().getLibelle())
                .consentementClient(demande.getConsentementClient())
                .build();
    }
}
