package com.eyram.dev.church_project_spring.utils;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Validation RIB / IBAN Togo (UEMOA) + cohérence avec le code banque (5 chiffres).
 * <p>
 * IBAN TG : 28 caractères ({@code TG + clé + TG + banque5 + guichet5 + compte11 + clé1}),
 * contrôle MOD-97. BBAN domestique : 24 caractères.
 */
public final class RibTogoValidator {

    private static final Map<String, String> BANK_CODES_BY_NAME = buildBankCodes();

    private RibTogoValidator() {
    }

    public record Result(boolean valid, String message, String normalized, String bankCode) {
        public static Result ok(String normalized, String bankCode, String message) {
            return new Result(true, message, normalized, bankCode);
        }

        public static Result fail(String message) {
            return new Result(false, message, null, null);
        }
    }

    public static Result validate(String rawRib, String nomBanque) {
        String compact = normalize(rawRib);
        if (!StringUtils.hasText(compact)) {
            return Result.fail("Le RIB / IBAN est obligatoire.");
        }

        String bankCode = null;
        String kind;

        if (compact.startsWith("TG") && compact.length() == 28) {
            if (!compact.matches("^TG\\d{2}TG\\d{22}$")) {
                return Result.fail(
                        "IBAN Togo invalide : format attendu TG + clé + TG + code banque (5) + guichet + compte (28 caractères)."
                );
            }
            if (!ibanMod97Valid(compact)) {
                return Result.fail("IBAN invalide : la clé de contrôle est incorrecte (erreur de saisie probable).");
            }
            bankCode = extractBankCode(compact);
            kind = "iban";
        } else if (compact.matches("^TG\\d{22}$") && compact.length() == 24) {
            bankCode = extractBankCode(compact);
            kind = "bban";
        } else if (compact.matches("^[A-Z0-9]{23}$")) {
            if (!frenchRibKeyValid(compact)) {
                return Result.fail("RIB invalide : la clé RIB (2 derniers chiffres) ne correspond pas au compte.");
            }
            kind = "rib-fr";
        } else {
            return Result.fail(
                    "Format non reconnu. Utilisez un IBAN Togo (28 car.) ou le RIB domestique (24 car., ex. TG00906…)."
            );
        }

        Optional<String> expected = expectedBankCode(nomBanque);
        if (expected.isPresent()) {
            if ("rib-fr".equals(kind)) {
                return Result.fail(
                        "Pour vérifier la banque, saisissez le RIB / IBAN au format Togo (commençant par TG…)."
                );
            }
            if (bankCode != null && !expected.get().equals(bankCode)) {
                return Result.fail(
                        "Ce RIB appartient à la banque " + bankCode
                                + ", pas à « " + nomBanque.trim()
                                + " » (code attendu " + expected.get() + ")."
                );
            }
        }

        String message = switch (kind) {
            case "iban" -> expected.isPresent()
                    ? "IBAN Togo valide et cohérent avec la banque."
                    : "IBAN Togo valide.";
            case "bban" -> expected.isPresent()
                    ? "RIB domestique Togo valide et cohérent avec la banque."
                    : "RIB domestique Togo valide.";
            default -> "RIB valide (clé correcte).";
        };
        return Result.ok(compact, bankCode, message);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toUpperCase(Locale.ROOT).replaceAll("[\\s\\-._]", "");
    }

    static String extractBankCode(String compact) {
        if (compact.startsWith("TG") && compact.length() == 28 && compact.startsWith("TG", 4)) {
            String code = compact.substring(6, 11);
            return code.matches("\\d{5}") ? code : null;
        }
        if (compact.matches("^TG\\d{22}$") && compact.length() == 24) {
            String code = compact.substring(2, 7);
            return code.matches("\\d{5}") ? code : null;
        }
        return null;
    }

    static boolean ibanMod97Valid(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder(rearranged.length() * 2);
        for (int i = 0; i < rearranged.length(); i++) {
            char ch = rearranged.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                numeric.append(ch - 55);
            } else if (ch >= '0' && ch <= '9') {
                numeric.append(ch);
            } else {
                return false;
            }
        }
        int remainder = 0;
        for (int i = 0; i < numeric.length(); i++) {
            remainder = (remainder * 10 + (numeric.charAt(i) - '0')) % 97;
        }
        return remainder == 1;
    }

    static boolean frenchRibKeyValid(String rib) {
        if (rib == null || rib.length() != 23) {
            return false;
        }
        String bank = rib.substring(0, 5);
        String branch = rib.substring(5, 10);
        String account = rib.substring(10, 21);
        int key;
        try {
            key = Integer.parseInt(rib.substring(21, 23));
        } catch (NumberFormatException ex) {
            return false;
        }
        String b = toFrenchRibNumber(bank);
        String g = toFrenchRibNumber(branch);
        String c = toFrenchRibNumber(account);
        if (b == null || g == null || c == null) {
            return false;
        }
        int expected = 97 - ((89 * mod(b, 97) + 15 * mod(g, 97) + 3 * mod(c, 97)) % 97);
        if (expected == 0) {
            expected = 97;
        }
        return expected == key;
    }

    private static String toFrenchRibNumber(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                out.append(ch);
            } else if (ch >= 'A' && ch <= 'I') {
                out.append(ch - 'A' + 1);
            } else if (ch >= 'J' && ch <= 'R') {
                out.append(ch - 'J' + 1);
            } else if (ch >= 'S' && ch <= 'Z') {
                out.append(ch - 'S' + 2);
            } else {
                return null;
            }
        }
        return out.toString();
    }

    private static int mod(String digits, int m) {
        int r = 0;
        for (int i = 0; i < digits.length(); i++) {
            r = (r * 10 + (digits.charAt(i) - '0')) % m;
        }
        return r;
    }

    private static Optional<String> expectedBankCode(String nomBanque) {
        if (!StringUtils.hasText(nomBanque)) {
            return Optional.empty();
        }
        String key = nomBanque.trim().toLowerCase(Locale.ROOT);
        String code = BANK_CODES_BY_NAME.get(key);
        return Optional.ofNullable(code);
    }

    private static Map<String, String> buildBankCodes() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, "Ecobank Togo", "00906");
        put(map, "Orabank Togo", "00904");
        put(map, "Banque Atlantique Togo", "00801");
        put(map, "Union Togolaise de Banque (UTB)", "00301");
        return Map.copyOf(map);
    }

    private static void put(Map<String, String> map, String name, String code) {
        map.put(name.toLowerCase(Locale.ROOT), code);
    }
}
