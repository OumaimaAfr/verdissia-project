--changeset 5-create-table-demande-client

CREATE TABLE IF NOT EXISTS demande_client (
    `id` CHAR(36) PRIMARY KEY,
    `client_id` BIGINT NOT NULL,
    `type_demande` VARCHAR(100) COMMENT '(souscription, résiliation, etc.)',
    `statut` ENUM('EN_ATTENTE', 'EN_COURS', 'VALIDEE', 'REJETEE', 'ANNULEE', 'EN_ERREUR'),
    `motif_rejet` TEXT ,
    `date_creation` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `date_traitement` DATETIME,
    FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE,
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--rollback drop table demande_client

