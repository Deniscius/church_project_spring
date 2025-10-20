package com.eyram.dev.church_project_spring.enums;

import java.time.DayOfWeek;

public enum TypeDemandeEnum {
    ACTION_DE_GRACE(DemandeCategorieEnum.SPECIALE, 5000),
    ANNIVERSAIRE   (DemandeCategorieEnum.SPECIALE, 5000),
    VEILLE         (DemandeCategorieEnum.SPECIALE, 5000),
    ENTERREMENT    (DemandeCategorieEnum.SPECIALE, 5000),
    ORDINATION     (DemandeCategorieEnum.SPECIALE, 5000),
    BAPTEME        (DemandeCategorieEnum.SPECIALE, 5000),
    MESSE_ORDINAIRE(DemandeCategorieEnum.ORDINAIRE, 3000); // défaut = dimanche

    private final DemandeCategorieEnum categorie;
    private final int prixFixe;

    TypeDemandeEnum(DemandeCategorieEnum categorie, int prixFixe) {
        this.categorie = categorie; this.prixFixe = prixFixe;
    }
    public DemandeCategorieEnum getCategorie() { return categorie; }
    public int getPrixFixe() { return prixFixe; }

    /** Prix réel selon le jour:
     *  - MESSE_ORDINAIRE: Dimanche=3000, Lundi–Samedi=1500
     *  - SPECIALE: 5000
     */
    public int getPrixPourJour(DayOfWeek day) {
        if (this == MESSE_ORDINAIRE) return (day == DayOfWeek.SUNDAY) ? 3000 : 1500;
        return prixFixe;
    }
}
