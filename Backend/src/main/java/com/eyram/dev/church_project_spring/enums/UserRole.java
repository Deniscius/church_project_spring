package com.eyram.dev.church_project_spring.enums;

/**
 * Rôles applicatifs (préfixe {@code ROLE_} ajouté par Spring Security).
 *
 * Hiérarchie des rôles (utilisateurs authentifiés) :
 * - SUPER_ADMIN : accès complet, gestion multi-paroisse
 * - COMPTABLE : comptable plateforme (finances SaaS)
 * - COMPTABLE_LOCAL : comptable local (trésorerie / caisse)
 * - ADMIN : administrateur local de paroisse
 * - SECRETAIRE : secrétaire de paroisse, gestion des demandes
 * - CURE : curé/prêtre, consultation et validation
 *
 * Note : Les fidèles n'ont pas de compte. Ils accèdent via endpoints publics anonymes.
 */
public enum UserRole {
    SECRETAIRE,        // Secrétaire de paroisse
    CURE,              // Curé/Prêtre
    ADMIN,             // Administrateur de paroisse (local)
    COMPTABLE_LOCAL,   // Comptable local (trésorerie / caisse)
    COMPTABLE,         // Comptable plateforme (finances SaaS)
    SUPER_ADMIN        // Super administrateur (global)
}
