package com.verdissia.repository;

import com.verdissia.model.DemandeClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeClientRepository extends JpaRepository<DemandeClient, String> {
    
    List<DemandeClient> findByClientId(Long clientId);
    
    List<DemandeClient> findByStatut(DemandeClient.StatutDemande statut);
    
    List<DemandeClient> findByClientEmail(String email);
    
    List<DemandeClient> findByOffreId(Integer offreId);

    List<DemandeClient> findByReferenceClient(String referenceClient);
}
