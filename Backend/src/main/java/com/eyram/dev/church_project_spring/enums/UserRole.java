package com.eyram.dev.church_project_spring.enums;

/**
 * Rôles <strong>applicatifs</strong> (préfixe {@code ROLE_} ajouté par Spring Security).
 * Stockés sur {@code users.role} — déterminent les droits dans l'app.
 *
 * <p>Ne pas confondre avec {@link RoleParoisse} (fonction dans une paroisse via
 * {@code paroisse_access}). Voir la javadoc de {@link RoleParoisse}.
 *
 * Hiérarchie :
 * <ul>
 *   <li>SUPER_ADMIN — plateforme, multi-tenant</li>
 *   <li>COMPTABLE — finances SaaS plateforme</li>
 *   <li>COMPTABLE_LOCAL — trésorerie / caisse paroissiale</li>
 *   <li>ADMIN — administrateur local de paroisse</li>
 *   <li>SECRETAIRE — demandes et encaissements</li>
 *   <li>CURE — consultation / validation</li>
 * </ul>
 *
 * Les fidèles n'ont pas de compte (parcours public anonyme).
 */
public enum UserRole {
    SECRETAIRE,
    CURE,
    ADMIN,
    COMPTABLE_LOCAL,
    COMPTABLE,
    SUPER_ADMIN
}
