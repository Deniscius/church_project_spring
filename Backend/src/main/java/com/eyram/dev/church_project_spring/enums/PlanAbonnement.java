package com.eyram.dev.church_project_spring.enums;

/**
 * Plans d'abonnement plateforme (XOF).
 */
public enum PlanAbonnement {
    MENSUEL(5_000, 1),
    SEMESTRIEL(8_000, 6),
    ANNUEL(12_000, 12);

    private final int montantXof;
    private final int dureeMois;

    PlanAbonnement(int montantXof, int dureeMois) {
        this.montantXof = montantXof;
        this.dureeMois = dureeMois;
    }

    public int getMontantXof() {
        return montantXof;
    }

    public int getDureeMois() {
        return dureeMois;
    }
}
