package com.verdissia.repository;

import com.verdissia.model.DemandeClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeClientRepository extends JpaRepository<DemandeClient, String> {
    
    List<DemandeClient> findByReferenceClient(String referenceClient);
}
