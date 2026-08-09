package com.eyram.dev.church_project_spring.service.payment.fedapay;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Normalise un téléphone fidèle (souvent E.164) pour l'API FedaPay Customer.
 * FedaPay attend number national sans indicatif ni 0 initial, + country ISO-2.
 */
public final class FedaPayPhone {

    private FedaPayPhone() {
    }

    public record Parts(String number, String country) {
    }

    /**
     * Indicatifs courants (ordre : plus longs d'abord pour éviter les collisions).
     */
    private static final String[][] CALLING_CODES = {
            {"228", "tg"}, // Togo
            {"229", "bj"}, // Bénin
            {"225", "ci"}, // Côte d'Ivoire
            {"226", "bf"}, // Burkina Faso
            {"227", "ne"}, // Niger
            {"221", "sn"}, // Sénégal
            {"223", "ml"}, // Mali
            {"224", "gn"}, // Guinée
            {"233", "gh"}, // Ghana
            {"234", "ng"}, // Nigeria
            {"237", "cm"}, // Cameroun
            {"241", "ga"}, // Gabon
            {"242", "cg"},
            {"243", "cd"},
            {"212", "ma"},
            {"213", "dz"},
            {"216", "tn"},
            {"33", "fr"},
            {"32", "be"},
            {"41", "ch"},
            {"1", "us"},
    };

    public static Parts parse(String raw, String defaultCountryIso2) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("[^0-9+]", "");
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }

        String defaultCountry = normalizeCountry(defaultCountryIso2, "tg");
        String digits = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned;
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }

        String country = defaultCountry;
        String national = digits;

        for (String[] entry : CALLING_CODES) {
            String cc = entry[0];
            if (digits.startsWith(cc) && digits.length() > cc.length() + 5) {
                country = entry[1];
                national = digits.substring(cc.length());
                break;
            }
        }

        // Format local type 09… / 0… → retirer les zéros de tête.
        national = national.replaceFirst("^0+", "");
        if (!StringUtils.hasText(national) || !national.matches("\\d{6,15}")) {
            return null;
        }

        return new Parts(national, country);
    }

    public static Map<String, Object> toCustomerPhone(String raw, String defaultCountryIso2) {
        Parts parts = parse(raw, defaultCountryIso2);
        if (parts == null) {
            return null;
        }
        Map<String, Object> phone = new LinkedHashMap<>();
        phone.put("number", parts.number());
        phone.put("country", parts.country());
        return phone;
    }

    private static String normalizeCountry(String iso2, String fallback) {
        if (!StringUtils.hasText(iso2)) {
            return fallback;
        }
        return iso2.trim().toLowerCase(Locale.ROOT);
    }
}
