
--changeset 3-create-table-offre

CREATE TABLE IF NOT EXISTS offre (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `libelle` VARCHAR(200) NOT NULL ,
    `description` TEXT ,
    `energie` ENUM('GAZ', 'ELECTRICITE', 'DUAL') NOT NULL,
    `prix_base` DECIMAL(10,6) ,
    `prix_abonnement_mensuel` DECIMAL(10,2),
    `offre_verte` BOOLEAN DEFAULT FALSE ,
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

--rollback drop table offre