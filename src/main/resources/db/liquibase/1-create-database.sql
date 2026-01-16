-- ============================================
-- Script de création de la base de données
-- Projet: Verdissia IA Backend
-- Version: 1.0
-- Date: 2026-01-16
-- ============================================

-- Suppression de la base si elle existe
DROP DATABASE IF EXISTS verdissia;

-- Création de la base de données
CREATE DATABASE verdissia
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Utilisation de la base
USE verdissia;

-- Configuration des paramètres de session
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET time_zone = '+00:00';
