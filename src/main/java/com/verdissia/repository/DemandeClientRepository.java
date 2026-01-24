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
    
    @Query("SELECT d FROM DemandeClient d WHERE d.client.email = :email")
    List<DemandeClient> findByClientEmail(@Param("email") String email);
    
    @Query("SELECT d FROM DemandeClient d WHERE d.offre.id = :offreId")
    List<DemandeClient> findByOffreId(@Param("offreId") Integer offreId);
}
