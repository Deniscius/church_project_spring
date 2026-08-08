package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Créneau de célébration (une date d'une série : triduum / neuvaine / trentaine). */
public record CelebrationSlotResponse(
        LocalDate date,
        Integer ordre,
        LocalTime heure,
        String horaireLibelle,
        UUID horairePublicId
) {
}
