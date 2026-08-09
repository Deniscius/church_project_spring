package com.eyram.dev.church_project_spring.utils;

import org.springframework.util.StringUtils;

/**
 * Masquage PII pour les réponses publiques (suivi / facture fidèle).
 */
public final class PiiMasking {

    private PiiMasking() {
    }

    public static String maskPhone(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "••••" + last4;
    }

    public static String maskEmail(String raw) {
        if (!StringUtils.hasText(raw) || !raw.contains("@")) {
            return null;
        }
        String email = raw.trim();
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (local.length() <= 1) {
            return "*" + "@" + domain;
        }
        return local.charAt(0) + "•••@" + domain;
    }
}
