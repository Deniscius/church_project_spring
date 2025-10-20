package com.eyram.dev.church_project_spring.enums;

public enum DureeMesseEnum {
    SIMPLE(1, 0),      // 0 => on utilisera le prix unitaire (jour-dépendant)
    TRIDUUM(3,  5000),
    NEUVAINE(9, 15000),
    TRENTAINE(30, 55000);

    private final int nbJours;
    private final int prixTotal;

    DureeMesseEnum(int nbJours, int prixTotal) {
        this.nbJours = nbJours; this.prixTotal = prixTotal;
    }
    public int getNbJours() { return nbJours; }
    public int getPrixTotal() { return prixTotal; }
    public boolean isPack() { return this != SIMPLE; }
}
