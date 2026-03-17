package com.eyram.dev.church_project_spring.enums;

public enum JourSemaine {

    DIMANCHE("Dimanche", 1),
    LUNDI("Lundi", 2),
    MARDI("Mardi", 3),
    MERCREDI("Mercredi", 4),
    JEUDI("Jeudi", 5),
    VENDREDI("Vendredi", 6),
    SAMEDI("Samedi", 7);

    private final String libelle;
    private final int ordre;

    JourSemaine(String libelle, int ordre) {
        this.libelle = libelle;
        this.ordre = ordre;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getOrdre() {
        return ordre;
    }
}