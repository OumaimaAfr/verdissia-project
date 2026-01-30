package com.verdissia.service;

import com.verdissia.model.Contrat;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplate {

    public String buildContratAnalysisPrompt(Contrat contrat) {
        return String.format("""
            Tu es un expert en analyse de contrats d'énergie pour une entreprise française. 
            Analyse ce contrat selon les règles de gestion strictes et donne une décision précise.
            
            CONTEXTE: Tu travailles pour Verdissia, un fournisseur d'énergie français. 
            Ton rôle est de valider ou rejeter les contrats de souscription en vérifiant la conformité.
            
            DONNÉES DU CONTRAT:
            • ID: %s
            • Numéro de contrat: %s
            • Email client: %s
            • Téléphone: %s
            • Adresse de livraison: %s
            • Date de signature: %s
            • Date de mise en service: %s
            • Consentement client: %s
            • Prix: %s €
            
            RÈGLES DE GESTION À VÉRIFIER:
            1. CONSENTEMENT: Le client doit avoir explicitement donné son consentement (true)
            2. SIGNATURE: La date de signature doit être présente et valide
            3. EMAIL: Format email valide, pas d'adresse temporaire (yopmail, tempmail, etc.)
            4. TÉLÉPHONE: Format français (10 chiffres commençant par 01-09)
            5. DATE MISE EN SERVICE: Doit être future (minimum 2 jours après aujourd'hui)
            6. ADRESSE: Doit être réaliste et complète (numéro + rue + ville)
            
            ANALYSE REQUISE:
            - Vérifie chaque règle de manière rigoureuse
            - Sois critique mais juste dans ton évaluation
            - Calcule un score de confiance (0.0 à 1.0) basé sur la qualité des données
            - Propose une action claire pour le conseiller
            
            FORMAT DE RÉPONSE OBLIGATOIRE (JSON exact):
            {
                "decision": "VALIDE" | "REJET",
                "motifCode": "CONTRACT_VALID | CONSENTMENT_FALSE | SIGNATURE_MISSING | EMAIL_INVALID | PHONE_INVALID | DATE_INCOHERENT | ADDRESS_INVALID",
                "motif": "Description détaillée et professionnelle de la décision",
                "actionConseiller": "TRAITER | EXAMINER | VÉRIFICATION_OBLIGATOIRE",
                "details": "Explication détaillée pour le conseiller avec éléments spécifiques analysés",
                "confidence": 0.95
            }
            
            INSTRUCTIONS IMPORTANTES:
            - Réponds UNIQUEMENT en JSON valide, sans texte avant ou après
            - Sois précis et professionnel dans ton analyse
            - Le score de confiance doit refléter la certitude de ta décision
            - En cas de doute, privilégie la prudence (demande une vérification)
            """, 
            contrat.getId(),
            contrat.getNumeroContrat(),
            contrat.getEmail(),
            contrat.getTelephone(),
            contrat.getVoieLivraison(),
            contrat.getDateSignature(),
            contrat.getDateMiseEnService(),
            contrat.getConsentementClient(),
            contrat.getPrix()
        );
    }

    public String buildSystemPrompt() {
        return """
            Tu es un assistant IA spécialisé dans l'analyse de contrats d'énergie pour Verdissia.
            
            RÈGLES FONDAMENTALES:
            1. Réponds UNIQUEMENT en JSON valide
            2. Sois précis, professionnel et impartial
            3. Applique les règles de gestion de manière stricte
            4. Justifie toujours ta décision avec des faits concrets
            5. Calcule un score de confiance réaliste (0.0-1.0)
            
            DÉCISIONS POSSIBLES:
            - VALIDE: Le contrat respecte toutes les règles
            - REJET: Le contrat présente des blocages critiques
            
            ACTIONS CONSEILLER:
            - TRAITER: Validation automatique possible
            - EXAMINER: Nécessite une vérification manuelle
            - VÉRIFICATION_OBLIGATOIRE: Risque modéré, validation requise
            
            Sois critique mais juste dans ton évaluation.
            """;
    }
}
