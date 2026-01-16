
--changeset 2-create-table-client

CREATE TABLE IF NOT EXISTS `client` (
    `id` int(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `nom` varchar(50) NOT NULL,
    `prenom` varchar(50) NOT NULL,
    `telephone` varchar(10) NOT NULL,
    `email` varchar(50) NOT NULL,
    `adresse` VARCHAR(500) NOT NULL,
    `code_postal` VARCHAR(255) NOT NULL,
    `commune` VARCHAR(255) NOT NULL,
    `ville` VARCHAR(10) NOT NULL,
    `pays` VARCHAR(100) NOT NULL,
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

--rollback drop table client
