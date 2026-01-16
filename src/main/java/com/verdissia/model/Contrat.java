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
@NoArgsConstructor
@AllArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_contrat", nullable = false, unique = true, length = 50)
    private String numeroContrat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_contrat_client"))
    private Client client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id",
            foreignKey = @ForeignKey(name = "fk_contrat_demande"))
    private DemandeClient demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id",
            foreignKey = @ForeignKey(name = "fk_contrat_offre"))
    private Offre typeOffre;

    @Enumerated(EnumType.STRING)
    @Column(name = "energie", nullable = false, length = 20)
    private Energie energie;

    @Column(name = "type_offre", length = 100)
    private String typeOffreLibelle;

    @Column(name = "adresse_livraison", nullable = false, length = 500)
    private String adresseLivraison;

    @Column(name = "code_postal_livraison", nullable = false, length = 10)
    private String codePostalLivraison;

    @Column(name = "ville_livraison", nullable = false, length = 100)
    private String villeLivraison;

    @Column(name = "prix", precision = 10, scale = 6)
    private BigDecimal prix;

    @Column(name = "prix_abonnement", precision = 10, scale = 2)
    private BigDecimal prixAbonnement;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StatutContrat statut = StatutContrat.BROUILLON;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    @Column(name = "date_activation")
    private LocalDateTime dateActivation;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    @Column(length = 34)
    private String iban;

    public enum Energie {
        GAZ, ELECTRICITE, DUAL
    }

    public enum StatutContrat {
        BROUILLON, EN_SIGNATURE, ACTIF, RESILIE, SUSPENDU, EXPIRE
    }

    public enum ModePaiement {
        PRELEVEMENT, VIREMENT, CARTE, CHEQUE
    }
}