--changeset 4-create-table-contrat

CREATE TABLE IF NOT EXISTS contrat (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `numero_contrat` VARCHAR(50) NOT NULL UNIQUE,
    `client_id` BIGINT NOT NULL,
    `demande_id` CHAR(36),
    `offre_id` INT,
    `enrgie` ENUM('GAZ', 'ELECTRICITE', 'DUAL') NOT NULL,
    `type_offre` VARCHAR(100),
    `adresse_livraison` VARCHAR(500) NOT NULL,
    `code_postal_livraison` VARCHAR(10) NOT NULL,
    `ville_livraison` VARCHAR(100) NOT NULL,
    `prix` DECIMAL(10,6),
    `prix_abonnement` DECIMAL(10,2),
    `statut` ENUM('BROUILLON', 'EN_SIGNATURE', 'ACTIF', 'RESILIE', 'SUSPENDU', 'EXPIRE'),
    `date_creation` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `date_signature` DATETIME,
    `date_activation` DATETIME,
    `mode_paiement` ENUM('PRELEVEMENT', 'VIREMENT', 'CARTE', 'CHEQUE'),
    `iban` VARCHAR(34),

    FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE RESTRICT,
    FOREIGN KEY (demande_id) REFERENCES demande_client(id) ON DELETE SET NULL,
    FOREIGN KEY (offre_id) REFERENCES offre(id) ON DELETE SET NULL,
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--rollback drop table contrat