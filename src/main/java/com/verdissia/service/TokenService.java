package com.verdissia.service;

import com.verdissia.dto.TokenValidationResult;
import com.verdissia.model.DemandeClient;
import com.verdissia.model.SignatureToken;
import com.verdissia.repository.SignatureTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public SignatureToken findActiveTokenOrGenerateNew(String email, DemandeClient demande) {
        // Check if client already has an active non-expired token
        List<SignatureToken> existingTokens = signatureTokenRepository
                .findActiveTokensByClientReference(demande.getReferenceClient(), LocalDateTime.now());
        
        if (!existingTokens.isEmpty()) {
            SignatureToken existingToken = existingTokens.get(0);
            log.info("Found existing active token {} for client reference {}", 
                    existingToken.getToken(), demande.getReferenceClient());
            return existingToken;
        }
        
        // No active token found, generate a new one
        log.info("No active token found for client reference {}, generating new token", 
                demande.getReferenceClient());
        return generateToken(email, demande);
    }

    public TokenValidationResult validateTokenWithDetails(String token) {
        SignatureToken signatureToken = signatureTokenRepository.findByToken(token)
                .orElse(null);

        if (signatureToken == null) {
            log.warn("Token not found: {}", token);
            return TokenValidationResult.invalid(
                TokenValidationResult.ValidationStatus.TOKEN_NOT_FOUND,
                "Ce lien de signature n'est pas valide. Veuillez vérifier que vous avez bien reçu le lien par email ou nous contacter pour assistance."
            );
        }

        // Check if token status is valid (ACTIF)
        if (signatureToken.getStatus() != SignatureToken.TokenStatus.ACTIF) {
            log.warn("Token status invalid: {} - Status: {}", token, signatureToken.getStatus());
            String message = signatureToken.getStatus() == SignatureToken.TokenStatus.UTILISE 
                ? "Votre signature a déjà été enregistrée. Vous n'avez plus besoin de signer ces documents."
                : "Ce lien de signature n'est plus actif. Veuillez nous contacter pour obtenir un nouveau lien si nécessaire.";
            return TokenValidationResult.invalid(
                TokenValidationResult.ValidationStatus.TOKEN_INVALID_STATUS,
                message
            );
        }

        // Check if token is expired (24h validity)
        if (signatureToken.isExpired()) {
            log.warn("Token expired: {} - Created: {}, Expired: {}", 
                    token, signatureToken.getDateCreation(), signatureToken.getDateExpiration());
            signatureToken.setStatus(SignatureToken.TokenStatus.EXPIRE);
            signatureTokenRepository.save(signatureToken);
            return TokenValidationResult.invalid(
                TokenValidationResult.ValidationStatus.TOKEN_EXPIRED,
                "Token expiré : votre lien de signature a expiré (validité de 24 heures). " +
                "Veuillez nous contacter au 01 23 45 67 89 pour obtenir un nouveau lien de signature."
            );
        }

        return TokenValidationResult.valid(signatureToken);
    }

    public SignatureToken validateToken(String token) {
        SignatureToken signatureToken = signatureTokenRepository.findByToken(token)
                .orElse(null);

        if (signatureToken == null) {
            log.warn("Token not found: {}", token);
            return null;
        }

        // Check if token status is valid (ACTIF)
        if (signatureToken.getStatus() != SignatureToken.TokenStatus.ACTIF) {
            log.warn("Token status invalid: {} - Status: {}", token, signatureToken.getStatus());
            return null;
        }

        // Check if token is expired (24h validity)
        if (signatureToken.isExpired()) {
            log.warn("Token expired: {} - Created: {}, Expired: {}", 
                    token, signatureToken.getDateCreation(), signatureToken.getDateExpiration());
            signatureToken.setStatus(SignatureToken.TokenStatus.EXPIRE);
            signatureTokenRepository.save(signatureToken);
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

    public void markTokensAsUsedForDemande(String demandeId) {
        List<SignatureToken> activeTokens = signatureTokenRepository.findActiveTokensByDemandeId(demandeId);
        
        for (SignatureToken token : activeTokens) {
            token.setStatus(SignatureToken.TokenStatus.UTILISE);
            token.setDateUtilisation(LocalDateTime.now());
            signatureTokenRepository.save(token);
        }
        
        log.info("Marked {} tokens as used for demande: {}", activeTokens.size(), demandeId);
    }
}
