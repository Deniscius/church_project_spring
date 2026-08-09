package com.eyram.dev.church_project_spring.enums;

/**
 * Rôle <strong>au sein d'une paroisse</strong> (table {@code paroisse_access}).
 * <p>
 * Ce n'est <em>pas</em> le rôle applicatif Spring Security ({@link UserRole}).
 * <ul>
 *   <li>{@link UserRole} → ce que l'utilisateur peut faire dans l'application
 *       (autorisations API, menus, guards).</li>
 *   <li>{@link RoleParoisse} → étiquette d'appartenance / fonction dans
 *       <em>cette</em> paroisse (équipe, affichage, fiche d'accès).</li>
 * </ul>
 * Exemple : un compte peut avoir {@code UserRole.SECRETAIRE} (droits métier)
 * et {@code RoleParoisse.GESTIONNAIRE} (libellé d'équipe dans la paroisse).
 * Aujourd'hui, la sécurité effective repose sur {@link UserRole} +
 * rattachement à la paroisse, pas sur la valeur de {@code RoleParoisse}.
 */
public enum RoleParoisse {
    /** Responsable local de la paroisse (équipe). */
    ADMIN,
    /** Responsable opérationnel (libellé d'équipe). */
    GESTIONNAIRE,
    /** Secrétariat de la paroisse. */
    SECRETAIRE,
    /** Accès en lecture / consultation (libellé d'équipe). */
    CONSULTATION
}
