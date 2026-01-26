package com.verdissia.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_contrat", nullable = false, unique = true, length = 50)
    private String numeroContrat;

    @Column(name = "reference_client", nullable = false, length = 100)
    private String referenceClient;

    @Column(nullable = false, length = 100)
    private String civilite;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_energie", nullable = false, length = 20)
    private Energie typeEnergie;

    @Column(name = "libelle_offre", nullable = false, length = 200)
    private String libelleOffre;

    @Column(name = "voie_livraison", nullable = false, length = 500)
    private String voieLivraison;

    @Column(name = "code_postal_livraison", nullable = false, length = 10)
    private String codePostalLivraison;

    @Column(name = "ville_livraison", nullable = false, length = 100)
    private String villeLivraison;

    @Column(name = "prix", precision = 10, scale = 6)
    private BigDecimal prix;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    @Column(name = "paiement_traite", length = 20)
    private Boolean paiementTraite;

    @Column(name = "date_mise_service")
    private LocalDateTime dateMiseEnService;

    @Column(name = "consentement_client", nullable = false)
    private Boolean consentementClient;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_llm", nullable = false, length = 20)
    @Builder.Default
    private StatutLlm statutLlm = StatutLlm.PENDING;

    public enum Energie {
        GAZ, ELECTRICITE, DUAL
    }

    public enum StatutLlm {
        PENDING, TRAITE
    }
}