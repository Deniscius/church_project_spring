package com.eyram.dev.church_project_spring.utils;

/**
 * Libellés métier des forfaits multi-jours (triduum / neuvaine / trentaine).
 */
public final class ForfaitDureeLabels {

    private ForfaitDureeLabels() {
    }

    public static String labelFor(Integer nombreCelebration) {
        if (nombreCelebration == null) {
            return "forfait";
        }
        return switch (nombreCelebration) {
            case 3 -> "triduum";
            case 9 -> "neuvaine";
            case 30 -> "trentaine";
            case 1 -> "célébration unique";
            default -> nombreCelebration + " célébrations";
        };
    }

    public static boolean isMultiCelebration(Integer nombreCelebration) {
        return nombreCelebration != null && nombreCelebration > 1;
    }
}
