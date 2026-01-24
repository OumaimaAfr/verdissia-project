package com.verdissia.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey qkey;

    private final long ttlMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttlMinutes:120}") long ttlMinutes
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is missing");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        }
        if (ttlMinutes <= 0) {
            throw new IllegalStateException("jwt.ttlMinutes must be > 0");
        }
        this.ttlMinutes = ttlMinutes;
        this.qkey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username) {
        return generate(username, Map.of());
    }

    public String generate(String username, Map<String, ?> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);

        return Jwts.builder()
                .claims(extraClaims == null ? Map.of() : extraClaims)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(qkey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(qkey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date exp = claims.getExpiration();
            if (exp == null || !exp.after(new Date())) {
                throw new IllegalArgumentException("Token expired");
            }
            return claims;
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
