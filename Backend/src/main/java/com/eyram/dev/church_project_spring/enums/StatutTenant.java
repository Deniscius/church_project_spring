package com.eyram.dev.church_project_spring.enums;

/**
 * Cycle de vie commercial d'une paroisse sur la plateforme.
 *
 * <p>À distinguer de {@link StatutAbonnement}, qui décrit une période facturée :
 * une paroisse traverse plusieurs abonnements successifs sans changer d'état, et
 * reste {@code EN_TOLERANCE} alors que son dernier abonnement est déjà expiré.
 *
 * <p>Le booléen {@code paroisse.is_active}, lu par l'authentification et par le
 * dépôt de demandes, est la projection de cet état : il vaut vrai pour
 * {@link #ACTIVE} et {@link #EN_TOLERANCE}, faux partout ailleurs. Une contrainte
 * de base garantit cette correspondance.
 */
public enum StatutTenant {

    /** Présente dans l'annuaire diocésain, jamais démarchée. Aucun compte. */
    PROSPECT,

    /** Dossier approuvé, comptes créés, premier encaissement attendu. */
    EN_ATTENTE_PAIEMENT,

    /** Abonnement en cours. */
    ACTIVE,

    /** Échéance dépassée, dans le délai de grâce : le service reste ouvert. */
    EN_TOLERANCE,

    /** Grâce épuisée : accès coupé, données conservées. */
    SUSPENDUE,

    /** Sortie du service à l'initiative de la paroisse ou de la plateforme. */
    RESILIEE;

    /**
     * Une paroisse peut constituer son équipe avant que l'abonnement ne démarre :
     * sans cela, activer un tenant supposerait d'y avoir déjà un administrateur,
     * qu'on ne peut justement pas y rattacher.
     */
    public boolean accepteConfigurationDesComptes() {
        return this == EN_ATTENTE_PAIEMENT || this == ACTIVE || this == EN_TOLERANCE;
    }
}
