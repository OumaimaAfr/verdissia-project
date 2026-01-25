package com.verdissia.model;

import com.verdissia.dto.request.DemandeClientRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeClient {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "reference_client", nullable = false, length = 50)
    private String referenceClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_demande_client"))
    private Client client;

    @Column(name = "type_demande", length = 100)
    private String typeDemande;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;

    @Column(name = "consentement_client", nullable = false)
    private Boolean consentementClient;

    public enum StatutDemande {
        EN_ATTENTE, EN_ATTENTE_SIGNATURE, EN_COURS, VALIDEE, REJETEE, ANNULEE, EN_ERREUR
    }

    public static DemandeClient fromRequest(DemandeClientRequest request, Client client, Offre offre) {
        DemandeClient d = new DemandeClient();
        d.typeDemande = request.getTypeDemande();
        d.referenceClient = request.getInformationsPersonnelles().getReferenceClient();
        d.client = client;
        d.offre = offre;
        d.consentementClient = request.getConsentementClient();

        // Statut initial
        d.statut = StatutDemande.EN_ATTENTE;

        d.dateCreation = LocalDateTime.from(Instant.now());
        d.dateTraitement = null;

        d.motifRejet = null;

        return d;

    }
}
