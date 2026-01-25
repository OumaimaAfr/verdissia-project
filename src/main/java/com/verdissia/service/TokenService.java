package com.verdissia.service;

import com.verdissia.model.DemandeClient;
import com.verdissia.model.SignatureToken;
import com.verdissia.repository.SignatureTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final SignatureTokenRepository signatureTokenRepository;

    public SignatureToken generateToken(String email, DemandeClient demande) {
        String tokenValue = UUID.randomUUID().toString().replace("-", "");
        
        SignatureToken token = SignatureToken.builder()
                .token(tokenValue)
                .email(email)
                .demande(demande)
                .dateCreation(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusHours(24))
                .status(SignatureToken.TokenStatus.ACTIF)
                .build();

        log.info("Generated token {} for email {} and demande {}", tokenValue, email, demande.getId());
        return signatureTokenRepository.save(token);
    }

    public SignatureToken validateToken(String token) {
        SignatureToken signatureToken = signatureTokenRepository.findByToken(token)
                .orElse(null);

        if (signatureToken == null) {
            log.warn("Token not found: {}", token);
            return null;
        }

        if (signatureToken.isExpired()) {
            log.warn("Token expired: {}", token);
            signatureToken.setStatus(SignatureToken.TokenStatus.EXPIRE);
            signatureTokenRepository.save(signatureToken);
            return null;
        }

        if (signatureToken.isUtilise()) {
            log.warn("Token already used: {}", token);
            return null;
        }

        return signatureToken;
    }

    public void markTokenAsUsed(String token) {
        SignatureToken signatureToken = signatureTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found: " + token));

        signatureToken.setStatus(SignatureToken.TokenStatus.UTILISE);
        signatureToken.setDateUtilisation(LocalDateTime.now());
        signatureTokenRepository.save(signatureToken);

        log.info("Token marked as used: {}", token);
    }
}
