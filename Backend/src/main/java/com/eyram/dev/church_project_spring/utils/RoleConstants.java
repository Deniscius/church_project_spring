package com.eyram.dev.church_project_spring.utils;

import java.util.Map;

import com.eyram.dev.church_project_spring.enums.UserRole;

/**
 * Constantes et mappages pour les rôles et permissions.
 * Centralisé pour faciliter la maintenance et les traductions.
 */
public class RoleConstants {

    // Noms de rôles avec le préfixe Spring Security
    public static final String ROLE_SECRETAIRE = "ROLE_SECRETAIRE";
    public static final String ROLE_CURE = "ROLE_CURE";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    // Labels localisables
    public static final Map<UserRole, String> ROLE_LABELS = Map.ofEntries(
            Map.entry(UserRole.SECRETAIRE, "Secrétaire"),
            Map.entry(UserRole.CURE, "Curé"),
            Map.entry(UserRole.ADMIN, "Administrateur"),
            Map.entry(UserRole.SUPER_ADMIN, "Super Administrateur")
    );

    // Descriptions des rôles
    public static final Map<UserRole, String> ROLE_DESCRIPTIONS = Map.ofEntries(
            Map.entry(UserRole.SECRETAIRE, "Secrétaire - Gère les demandes et validations de sa paroisse"),
            Map.entry(UserRole.CURE, "Curé - Consultations et validations des demandes"),
            Map.entry(UserRole.ADMIN, "Administrateur - Gestion complète de sa paroisse et ses utilisateurs"),
            Map.entry(UserRole.SUPER_ADMIN, "Super Administrateur - Gestion système complète, multi-paroisse")
    );

    // Codes d'erreur
    public static final String ERROR_UNAUTHORIZED_ROLE = "err.unauthorized.role";
    public static final String ERROR_INSUFFICIENT_PERMISSIONS = "err.insufficient.permissions";
    public static final String ERROR_TENANT_MISMATCH = "err.tenant.mismatch";

    /**
     * Retourne le label d'un rôle.
     */
    public static String getLabel(UserRole role) {
        return ROLE_LABELS.getOrDefault(role, role.name());
    }

    /**
     * Retourne la description d'un rôle.
     */
    public static String getDescription(UserRole role) {
        return ROLE_DESCRIPTIONS.getOrDefault(role, "");
    }

    /**
     * Vérifie si deux rôles sont du même niveau hiérarchique ou supérieur.
     * Hiérarchie : SUPER_ADMIN > ADMIN > SECRETAIRE > CURE
     * 
     * @param userRole le rôle de l'utilisateur
     * @param requiredRole le rôle requis
     * @return true si userRole >= requiredRole dans la hiérarchie
     */
    public static boolean isHierarchyGreaterOrEqual(UserRole userRole, UserRole requiredRole) {
        final int[] hierarchy = new int[4];
        hierarchy[UserRole.CURE.ordinal()] = 1;
        hierarchy[UserRole.SECRETAIRE.ordinal()] = 2;
        hierarchy[UserRole.ADMIN.ordinal()] = 3;
        hierarchy[UserRole.SUPER_ADMIN.ordinal()] = 4;

        return hierarchy[userRole.ordinal()] >= hierarchy[requiredRole.ordinal()];
    }
}
