package com.eyram.dev.church_project_spring.enums;

import java.time.DayOfWeek;

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

    public static JourSemaine fromDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> DIMANCHE;
            case MONDAY -> LUNDI;
            case TUESDAY -> MARDI;
            case WEDNESDAY -> MERCREDI;
            case THURSDAY -> JEUDI;
            case FRIDAY -> VENDREDI;
            case SATURDAY -> SAMEDI;
        };
    }

    @Override
    public String toString() {
        return libelle;
    }
}