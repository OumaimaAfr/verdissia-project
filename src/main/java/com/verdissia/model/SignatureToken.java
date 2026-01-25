package com.verdissia.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", unique = true, nullable = false, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeClient demande;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIF;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "date_utilisation")
    private LocalDateTime dateUtilisation;

    public enum TokenStatus {
        ACTIF, UTILISE, EXPIRE
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

    public boolean isUtilise() {
        return TokenStatus.UTILISE.equals(status);
    }
}
