package com.eyram.dev.church_project_spring.utils;

import org.springframework.util.StringUtils;

/**
 * Affichage / normalisation du nom du fidèle sur les demandes.
 * <ul>
 *   <li>prénom seul → prénom</li>
 *   <li>nom seul → nom</li>
 *   <li>les deux → « prénom nom »</li>
 *   <li>rien → {@link #DEFAULT}</li>
 * </ul>
 */
public final class FideleNameUtils {

    public static final String DEFAULT = "Un(e) chrétien(ne)";

    private FideleNameUtils() {
    }

    public static String format(String prenom, String nom) {
        boolean hasPrenom = StringUtils.hasText(prenom);
        boolean hasNom = StringUtils.hasText(nom);
        if (!hasPrenom && !hasNom) {
            return DEFAULT;
        }
        if (!hasPrenom) {
            return nom.trim();
        }
        if (!hasNom) {
            return prenom.trim();
        }
        return prenom.trim() + " " + nom.trim();
    }
}
