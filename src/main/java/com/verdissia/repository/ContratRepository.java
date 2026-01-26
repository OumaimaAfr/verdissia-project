package com.verdissia.repository;

import com.verdissia.model.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {
    
    Optional<Contrat> findByReferenceClient(String referenceClient);
    
    boolean existsByReferenceClient(String referenceClient);
    
    Optional<Contrat> findByEmail(String email);
}
