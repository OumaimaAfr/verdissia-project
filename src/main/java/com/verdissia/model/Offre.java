package com.verdissia.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Column(name = "type_energie", nullable = false, length = 20)
    private TypeEnergie typeEnergie;

    @Column(nullable = false, length = 20)
    private String preferenceOffre;

    @Column(name = "prix", precision = 10, scale = 6)
    private BigDecimal prix;

    @Column(name = "date_mise_service")
    private LocalDateTime dateMiseEnService;

    public enum TypeEnergie {
        GAZ, ELECTRICITE, DUAL
    }
}