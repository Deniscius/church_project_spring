package com.eyram.dev.church_project_spring.enums;

public enum JourSemaineEnum {
    DIMANCHE(0),
    LUNDI(1),
    MARDI(2),
    MERCREDI(3),
    JEUDI(4),
    VENDREDI(5),
    SAMEDI(6);

    private final int index;

    JourSemaineEnum(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static JourSemaineEnum fromIndex(int index) {
        for (JourSemaineEnum jour : values()) {
            if (jour.index == index) {
                return jour;
            }
        }
        throw new IllegalArgumentException("Index invalide pour JourSemaineEnum : " + index);
    }

}


