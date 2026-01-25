package com.verdissia.repository;

import com.verdissia.model.SignatureToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignatureTokenRepository extends JpaRepository<SignatureToken, Long> {
    Optional<SignatureToken> findByToken(String token);
}
