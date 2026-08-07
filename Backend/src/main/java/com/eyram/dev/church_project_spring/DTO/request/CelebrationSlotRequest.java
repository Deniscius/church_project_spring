package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Créneau choisi pour une date de célébration (multi-jours : triduum / neuvaine / trentaine).
 */
public record CelebrationSlotRequest(

        @NotNull(message = "La date de célébration est obligatoire")
        LocalDate date,

        UUID horairePublicId,

        LocalTime heurePersonnalisee
) {
}
