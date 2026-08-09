package com.eyram.dev.church_project_spring.utils;

import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalisation et validation d'une intention de messe (anti-saisie fantaisiste).
 * Aligné sur {@code Frontend/.../utils/intentionText.js}.
 */
public final class IntentionTextUtils {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 500;

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern LETTER = Pattern.compile("\\p{L}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern REPEAT = Pattern.compile("(.)\\1{4,}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern URL = Pattern.compile("(?i)https?://|www\\.");
    private static final Pattern NON_LETTER_SPACE = Pattern.compile("[^\\p{L}\\s']", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private static final Set<String> JUNK = Set.of(
            "test", "testing", "teste", "essai",
            "xxx", "xxxx", "aaaa", "bbbb",
            "asdf", "asdfgh", "qwerty", "azerty",
            "lorem", "ipsum",
            "nimp", "nimporte", "n importe", "n'importe",
            "blah", "bla", "blabla",
            "rien", "aucun", "aucune",
            "oui", "non", "ok",
            "hello", "salut", "..."
    );

    private IntentionTextUtils() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return MULTI_SPACE.matcher(value.strip()).replaceAll(" ").trim();
    }

    /**
     * @return intention normalisée
     * @throws BusinessRuleException si invalide
     */
    public static String requireValid(String raw) {
        String text = normalize(raw);
        String error = validate(text);
        if (error != null) {
            throw new BusinessRuleException(error);
        }
        return text;
    }

    /** Message d'erreur ou {@code null} si OK (texte déjà normalisé recommandé). */
    public static String validate(String text) {
        if (!StringUtils.hasText(text)) {
            return "L'intention est obligatoire — indiquez pour qui ou pour quelle intention.";
        }
        if (text.length() < MIN_LENGTH) {
            return "Précisez un peu plus l'intention (au moins " + MIN_LENGTH + " caractères).";
        }
        if (text.length() > MAX_LENGTH) {
            return "L'intention ne doit pas dépasser " + MAX_LENGTH + " caractères.";
        }

        int letters = countLetters(text);
        if (letters < 5) {
            return "L'intention doit contenir des mots (pas seulement des chiffres ou signes).";
        }

        String nonSpace = text.replace(" ", "");
        if (!nonSpace.isEmpty() && (double) letters / nonSpace.length() < 0.45) {
            return "Formulez une intention lisible (évitez les suites de chiffres ou de symboles).";
        }

        if (REPEAT.matcher(nonSpace).find()) {
            return "Cette intention ne semble pas valide (caractères répétés).";
        }

        if (URL.matcher(text).find()) {
            return "Une intention de messe ne doit pas contenir de lien internet.";
        }

        if (isJunkPhrase(text)) {
            return "Indiquez une intention réelle (ex. « Pour le repos de l'âme de… » ou « Action de grâce »).";
        }

        return null;
    }

    private static int countLetters(String text) {
        int n = 0;
        var matcher = LETTER.matcher(text);
        while (matcher.find()) {
            n++;
        }
        return n;
    }

    private static boolean isJunkPhrase(String text) {
        String compact = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        compact = MARKS.matcher(compact).replaceAll("");
        compact = NON_LETTER_SPACE.matcher(compact).replaceAll(" ");
        compact = MULTI_SPACE.matcher(compact).replaceAll(" ").trim();
        if (!StringUtils.hasText(compact)) {
            return true;
        }
        if (JUNK.contains(compact)) {
            return true;
        }
        String[] parts = compact.split(" ");
        if (parts.length == 0) {
            return true;
        }
        for (String part : parts) {
            if (!JUNK.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
