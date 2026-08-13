package com.eyram.dev.church_project_spring.enums;

/**
 * Catégories liturgiques des types de demande.
 * Alignées sur le CHECK SQL {@code type_principal}.
 */
public enum TypeDemandeEnum {
    EUCHARISTIE("Eucharistie"),
    SACRAMENT("Sacrement"),
    SACRAMENTAUX("Sacramentaux");

    private final String libelle;

    TypeDemandeEnum(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
