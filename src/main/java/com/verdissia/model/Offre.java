package com.verdissia.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "offre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "energie", nullable = false, length = 20)
    private TypeEnergie energie;

    @Column(name = "prix_base", precision = 10, scale = 6)
    private BigDecimal prixBase;

    @Column(name = "prix_abonnement_mensuel", precision = 10, scale = 2)
    private BigDecimal prixAbonnementMensuel;

    @Column(name = "offre_verte")
    private Boolean offreVerte = false;

    public enum TypeEnergie {
        GAZ, ELECTRICITE, DUAL
    }
}