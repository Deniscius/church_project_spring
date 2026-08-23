package com.eyram.dev.church_project_spring.utils;

import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.text.Normalizer;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Génère des codes métier lisibles (préfixe + initiales paroisse + horodatage)
 * avec un suffixe aléatoire pour limiter l'énumération des codes de suivi publics.
 */
public final class BusinessCodeGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Set<String> STOP_WORDS = Set.of(
            "DE", "DU", "DES", "LA", "LE", "LES", "ET", "EN", "AU", "AUX",
            "PAROISSE", "EGLISE", "CATHEDRALE", "BASILIQUE", "CHAPELLE"
    );

    private BusinessCodeGenerator() {
    }

    public static String parishInitials(String parishName) {
        if (parishName == null || parishName.isBlank()) {
            return "XXX";
        }

        String normalized = Normalizer.normalize(parishName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9\\s-]", " ")
                .replace('-', ' ')
                .trim();

        StringBuilder initials = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isEmpty() || STOP_WORDS.contains(part)) {
                continue;
            }
            if (part.equals("SAINT") || part.equals("SAINTE") || part.equals("STS") || part.equals("STE")) {
                initials.append('S');
            } else {
                initials.append(part.charAt(0));
            }
            if (initials.length() >= 4) {
                break;
            }
        }

        if (initials.isEmpty()) {
            String compact = normalized.replaceAll("\\s+", "");
            initials.append(compact, 0, Math.min(3, Math.max(compact.length(), 0)));
        }
        while (initials.length() < 2) {
            initials.append('X');
        }
        return initials.toString();
    }

    public static String timestamp(LocalDateTime when) {
        return (when != null ? when : LocalDateTime.now()).format(TS);
    }

    /** Jeton non prédictible sur un alphabet de 32 symboles (5 bits par caractère). */
    public static String publicToken(int length) {
        int size = Math.max(6, Math.min(length, 16));
        char[] buf = new char[size];
        for (int i = 0; i < size; i++) {
            buf[i] = TOKEN_ALPHABET[SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length)];
        }
        return new String(buf);
    }

    public static String typeAbbrev(TypeDemandeEnum type) {
        if (type == null) {
            return "GEN";
        }
        return switch (type) {
            case EUCHARISTIE -> "EUC";
            case SACRAMENT -> "SAC";
            case SACRAMENTAUX -> "SAX";
        };
    }

    public static String natureAbbrev(NatureForfaitEnum nature) {
        if (nature == null) {
            return "NOR";
        }
        return switch (nature) {
            case NORMALE -> "NOR";
            case DOMINICALE -> "DOM";
            case SPECIALE -> "SPE";
        };
    }

    /**
     * Code de suivi : MS + initiales paroisse + jeton aléatoire.
     * Exemple paroisse « Saint Joseph » → {@code MS-SJ-K7M2XQ8W4P}.
     */
    public static String demandeCode(String parishName) {
        String initials = parishInitials(parishName);
        return "MS-" + initials + "-" + publicToken(10);
    }

    /**
     * Normalise la saisie utilisateur (casse, espaces, tirets) vers le format stocké.
     * Accepte {@code MS-SJ-K7M2XQ}, {@code MSSJK7M2XQ}, l’ancien {@code MS-K7M2XQ},
     * et les codes longs historiques {@code DEM-…}.
     */
    public static String normalizeDemandeTrackingCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        String compact = trimmed.replaceAll("[\\s_-]+", "");

        // Nouveau format renforcé : MS + initiales (2–4 lettres) + jeton 10 (50 bits).
        if (compact.matches("MS[A-Z]{2,4}[A-Z2-9]{10}")) {
            String body = compact.substring(2);
            String token = body.substring(body.length() - 10);
            String initials = body.substring(0, body.length() - 10);
            return "MS-" + initials + "-" + token;
        }
        // Format 6 caractères historique : conservé uniquement pour les demandes existantes.
        if (compact.matches("MS[A-Z]{2,4}[A-Z2-9]{6}")) {
            String body = compact.substring(2);
            String token = body.substring(body.length() - 6);
            String initials = body.substring(0, body.length() - 6);
            return "MS-" + initials + "-" + token;
        }
        // Ancien format court : MS + jeton 6 uniquement
        if (compact.matches("MS[A-Z2-9]{6}")) {
            return "MS-" + compact.substring(2);
        }
        if (compact.matches("[A-Z2-9]{6}")) {
            return "MS-" + compact;
        }
        return trimmed.replaceAll("\\s+", "");
    }

    /** Ex. FAC-SPL-20260730-145123-K7M2XQ */
    public static String factureCode(String parishName) {
        return "FAC-" + parishInitials(parishName) + "-" + timestamp(LocalDateTime.now())
                + "-" + publicToken(6);
    }

    /** Ex. TXN-SPL-20260730-145123-K7M2XQ */
    public static String transactionCode(String parishName) {
        return "TXN-" + parishInitials(parishName) + "-" + timestamp(LocalDateTime.now())
                + "-" + publicToken(6);
    }

    /**
     * Ex. FOR-SPL-EUC-NOR-9 (9 = nombre de célébrations, sinon jours, sinon 1).
     */
    public static String forfaitCode(
            String parishName,
            TypeDemandeEnum type,
            NatureForfaitEnum nature,
            Integer nombreCelebration,
            Integer nombreJour
    ) {
        int duree = nombreCelebration != null && nombreCelebration > 0
                ? nombreCelebration
                : (nombreJour != null && nombreJour > 0 ? nombreJour : 1);
        return "FOR-"
                + parishInitials(parishName)
                + "-"
                + typeAbbrev(type)
                + "-"
                + natureAbbrev(nature)
                + "-"
                + duree;
    }

    /**
     * Garantit l'unicité : base logique, puis suffixe -01, -02… en cas de collision.
     */
    public static String unique(String baseCode, Predicate<String> alreadyExists) {
        if (baseCode == null || baseCode.isBlank()) {
            throw new IllegalArgumentException("Le code de base est obligatoire");
        }
        if (!alreadyExists.test(baseCode)) {
            return baseCode;
        }
        for (int i = 1; i <= 99; i++) {
            String candidate = baseCode + "-" + String.format(Locale.ROOT, "%02d", i);
            if (!alreadyExists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de générer un code unique pour : " + baseCode);
    }
}
