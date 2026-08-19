package com.eyram.dev.church_project_spring.enums;

/**
 * Origine du programme résolu d'une journée.
 */
public enum ModeProgrammeJour {
    /** Grille hebdomadaire uniquement. */
    HEBDOMADAIRE,
    /** Grille hebdomadaire complétée par un ou plusieurs créneaux ponctuels. */
    HEBDOMADAIRE_AVEC_AJOUT,
    /** Grille entièrement personnalisée pour cette date. */
    PERSONNALISE,
    /** Une célébration ponctuelle unique remplace tous les autres créneaux. */
    MESSE_UNIQUE
}
