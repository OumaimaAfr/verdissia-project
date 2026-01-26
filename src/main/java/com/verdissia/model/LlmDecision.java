package com.verdissia.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_decision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision; // REJET, VALIDE

    @Column(name = "motif_code", nullable = false, length = 50)
    private String motifCode; // CONSENTMENT_FALSE, EMAIL_INVALID, PHONE_INVALID, etc.

    @Column(name = "motif_message", nullable = false, columnDefinition = "TEXT")
    private String motifMessage; // Message détaillé du motif

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence; // Score de confiance 0.00-1.00

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private ProcessStatus processStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "action_conseiller", length = 50)
    private String actionConseiller; // EXAMINER, TRAITER

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Détails complémentaires

    public enum ProcessStatus {
        SUCCESS, ERROR, PENDING
    }
}
