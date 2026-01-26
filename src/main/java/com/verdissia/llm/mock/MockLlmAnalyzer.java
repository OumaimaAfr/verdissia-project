package com.verdissia.llm.mock;

import com.verdissia.model.Contrat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MockLlmAnalyzer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LlmAnalysisResult analyzeContrat(Contrat contrat) {
        log.info("Début de l'analyse LLM pour le contrat {} - {}", contrat.getId(), contrat.getNumeroContrat());

        // Règle spéciale: Si le score de confiance est < 0.50, demander une revue manuelle
        // Cette règle s'applique après toutes les vérifications de base
        LlmAnalysisResult preliminaryResult = performBasicAnalysis(contrat);
        
        // Simuler un calcul de confiance basé sur la complexité du cas
        BigDecimal confidence = calculateConfidence(contrat, preliminaryResult);
        
        if (confidence.compareTo(new BigDecimal("0.50")) < 0) {
            log.warn("Confiance très faible ({}) pour le contrat {} - Revue manuelle requise", confidence, contrat.getId());
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("REVUE_MANUELLE")
                    .motif("Score de confiance insuffisant - Revue manuelle requise")
                    .actionConseiller("EXAMINER")
                    .details("Le score de confiance de " + confidence + " est inférieur au seuil de 0.50. Une revue manuelle par un conseiller est nécessaire.")
                    .confidence(confidence)
                    .build();
        }
        
        if (confidence.compareTo(new BigDecimal("0.60")) < 0) {
            log.warn("Confiance faible ({}) pour le contrat {} - Vérification obligatoire requise", confidence, contrat.getId());
            return LlmAnalysisResult.builder()
                    .decision("VALIDE")
                    .motifCode("VERIFICATION_OBLIGATOIRE")
                    .motif("Score de confiance faible - Vérification obligatoire requise")
                    .actionConseiller("VÉRIFICATION_OBLIGATOIRE")
                    .details("Le score de confiance de " + confidence + " est inférieur au seuil de 0.60. Une vérification obligatoire par un conseiller est nécessaire avant de traiter la demande.")
                    .confidence(confidence)
                    .build();
        }
        
        return LlmAnalysisResult.builder()
                .decision(preliminaryResult.getDecision())
                .motifCode(preliminaryResult.getMotifCode())
                .motif(preliminaryResult.getMotif())
                .actionConseiller(preliminaryResult.getActionConseiller())
                .details(preliminaryResult.getDetails())
                .confidence(confidence)
                .build();
    }
    
    private LlmAnalysisResult performBasicAnalysis(Contrat contrat) {
        // RG 1: Vérification du consentement client
        if (contrat.getConsentementClient() == null || !contrat.getConsentementClient()) {
            log.warn("RG KO - Consentement client manquant ou faux");
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("CONSENTMENT_FALSE")
                    .motif("Consentement client obligatoire non coché")
                    .actionConseiller("EXAMINER")
                    .details("Le client n'a pas donné son consentement pour la souscription du contrat")
                    .build();
        }

        // RG 2: Vérification de la date de signature
        if (contrat.getDateSignature() == null) {
            log.warn("RG KO - Date de signature manquante");
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("SIGNATURE_MISSING")
                    .motif("Date de signature manquante")
                    .actionConseiller("EXAMINER")
                    .details("Le contrat doit être signé pour être valide")
                    .build();
        }

        // RG 3: Vérification de l'email
        if (!isValidEmail(contrat.getEmail())) {
            log.warn("RG KO - Email invalide: {}", contrat.getEmail());
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("EMAIL_INVALID")
                    .motif("Format d'email invalide")
                    .actionConseiller("EXAMINER")
                    .details("L'email '" + contrat.getEmail() + "' n'est pas valide ou provient d'un service temporaire")
                    .build();
        }

        // RG 4: Vérification du téléphone
        if (!isValidPhone(contrat.getTelephone())) {
            log.warn("RG KO - Téléphone invalide: {}", contrat.getTelephone());
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("PHONE_INVALID")
                    .motif("Numéro de téléphone invalide")
                    .actionConseiller("EXAMINER")
                    .details("Le numéro de téléphone '" + contrat.getTelephone() + "' n'est pas un format valide")
                    .build();
        }

        // RG 5: Vérification de la date de mise en service
        if (isIncoherentDate(contrat.getDateMiseEnService())) {
            log.warn("RG KO - Date de mise en service incohérente: {}", contrat.getDateMiseEnService());
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("DATE_INCOHERENT")
                    .motif("Date de mise en service incohérente")
                    .actionConseiller("EXAMINER")
                    .details("La date de mise en service ne peut pas être antérieure ou égale à aujourd'hui")
                    .build();
        }

        // RG 6: Vérification de l'adresse
        if (!isValidAddress(contrat.getVoieLivraison())) {
            log.warn("RG KO - Adresse invalide: {}", contrat.getVoieLivraison());
            return LlmAnalysisResult.builder()
                    .decision("REJET")
                    .motifCode("ADDRESS_INVALID")
                    .motif("Adresse de livraison invalide")
                    .actionConseiller("EXAMINER")
                    .details("L'adresse de livraison '" + contrat.getVoieLivraison() + "' ne semble pas être une adresse réelle")
                    .build();
        }

        // Si toutes les RG sont validées
        log.info("RG OK - Contrat validé");
        return LlmAnalysisResult.builder()
                .decision("VALIDE")
                .motifCode("CONTRACT_VALID")
                .motif("Contrat conforme aux règles de gestion")
                .actionConseiller("TRAITER")
                .details("Toutes les informations du contrat sont valides et conformes")
                .build();
    }
    
    private BigDecimal calculateConfidence(Contrat contrat, LlmAnalysisResult preliminaryResult) {
        // Calcul du score de confiance basé sur plusieurs facteurs
        
        double confidence = 1.0; // Score de base
        
        // Réduire la confiance si des champs sont manquants ou suspects
        if (preliminaryResult.getDecision().equals("REJET")) {
            switch (preliminaryResult.getMotifCode()) {
                case "CONSENTMENT_FALSE":
                    confidence = 0.95; // Très sûr du rejet
                    break;
                case "EMAIL_INVALID":
                    confidence = 0.85; // Assez sûr
                    break;
                case "PHONE_INVALID":
                    confidence = 0.80; // Moyennement sûr
                    break;
                case "DATE_INCOHERENT":
                    confidence = 0.75; // Moins sûr
                    break;
                case "ADDRESS_INVALID":
                    confidence = 0.70; // Peu sûr
                    break;
                case "SIGNATURE_MISSING":
                    confidence = 0.90; // Sûr
                    break;
            }
        } else {
            // Cas valide - vérifier la qualité des données
            if (contrat.getEmail() != null && contrat.getEmail().contains("test")) {
                confidence -= 0.30; // Email de test
            }
            if (contrat.getTelephone() != null && contrat.getTelephone().startsWith("06")) {
                confidence -= 0.10; // Portable (moins fiable)
            }
            if (contrat.getVoieLivraison() != null && contrat.getVoieLivraison().length() < 10) {
                confidence -= 0.40; // Adresse trop courte
            }
            if (contrat.getPrix() != null && contrat.getPrix().compareTo(new BigDecimal("50")) < 0) {
                confidence -= 0.25; // Prix très bas (suspect)
            }
        }
        
        // Ajouter un facteur aléatoire pour simuler l'incertitude du LLM
        confidence -= (Math.random() * 0.2);
        
        // S'assurer que le score reste dans les limites
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        
        // Utiliser un formatage US pour éviter les virgules françaises
        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return new BigDecimal(df.format(confidence));
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Vérification du format de base
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        // Vérification des emails temporaires/fake
        String lowerEmail = email.toLowerCase();
        if (lowerEmail.contains("@yopmail") || 
            lowerEmail.contains("@tempmail") || 
            lowerEmail.contains("@10minutemail") ||
            lowerEmail.contains("@mailinator") ||
            lowerEmail.contains("@guerrillamail") ||
            lowerEmail.contains("@throwaway")) {
            return false;
        }

        // Vérification des @@ ou formats incohérents
        if (email.contains("@@") || email.contains("..") || email.startsWith("@") || email.endsWith("@")) {
            return false;
        }

        return true;
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Nettoyage du numéro (suppression des espaces, tirets, points)
        String cleanPhone = phone.replaceAll("[\\s\\-\\.]", "");
        
        // Vérification du format français (10 chiffres commençant par 06, 07, ou 01-09)
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return false;
        }

        // Vérification que le numéro commence par un préfixe valide
        String prefix = cleanPhone.substring(0, 2);
        return prefix.startsWith("06") || prefix.startsWith("07") || 
               prefix.startsWith("01") || prefix.startsWith("02") || 
               prefix.startsWith("03") || prefix.startsWith("04") || 
               prefix.startsWith("05") || prefix.startsWith("08") || 
               prefix.startsWith("09");
    }

    private boolean isIncoherentDate(LocalDateTime dateMiseEnService) {
        if (dateMiseEnService == null) {
            return true; // Date manquante = incohérente
        }

        LocalDateTime now = LocalDateTime.now();
        
        // La date de mise en service ne doit pas être antérieure ou égale à aujourd'hui
        // Elle doit être au minimum dans 2 jours pour être réaliste
        LocalDateTime minDate = now.plusDays(2);
        
        return !dateMiseEnService.isAfter(minDate);
    }

    private boolean isValidAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }

        String cleanAddress = address.trim().toLowerCase();

        // Vérification des adresses明显 fake
        if (cleanAddress.contains("test") || 
            cleanAddress.contains("fake") || 
            cleanAddress.contains("demo") ||
            cleanAddress.contains("exemple") ||
            cleanAddress.contains("rue de la paix") && cleanAddress.length() < 20 ||
            cleanAddress.matches("^[0-9]+\\s*[a-z]{1,5}\\s*$")) { // Format trop simple comme "1 rue"
            return false;
        }

        // Vérification que l'adresse contient au minimum des éléments réalistes
        // (numéro + nom de rue + ville)
        return cleanAddress.length() >= 10 && 
               cleanAddress.matches(".*[0-9]+.*") && 
               cleanAddress.matches(".*[a-z]{3,}.*");
    }
}
