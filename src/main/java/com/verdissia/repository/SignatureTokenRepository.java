package com.verdissia.repository;

import com.verdissia.model.SignatureToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureTokenRepository extends JpaRepository<SignatureToken, Long> {
    Optional<SignatureToken> findByToken(String token);
    
    // Use native query to avoid Spring Data JPA query processing issues
    @Query(value = "SELECT st.* FROM signature_token st " +
                   "JOIN demande_client dc ON st.demande_id = dc.id " +
                   "WHERE dc.reference_client = :referenceClient " +
                   "AND st.status = 'ACTIF' " +
                   "AND st.date_expiration > :now " +
                   "ORDER BY st.date_creation DESC", nativeQuery = true)
    List<SignatureToken> findActiveTokensByClientReference(@Param("referenceClient") String referenceClient, 
                                                          @Param("now") LocalDateTime now);
    
    @Query(value = "SELECT * FROM signature_token " +
                   "WHERE demande_id = :demandeId " +
                   "AND status = 'ACTIF'", nativeQuery = true)
    List<SignatureToken> findActiveTokensByDemandeId(@Param("demandeId") String demandeId);
}
