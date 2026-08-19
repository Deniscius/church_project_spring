package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Modification ciblée du créneau d'une célébration déjà programmée.
 *
 * La date n'est volontairement pas modifiable ici : changer de jour peut avoir
 * un impact tarifaire (honoraire dominical / spécial). Cette action ne change
 * donc que le créneau sur la journée déjà choisie.
 */
public record CelebrationScheduleUpdateRequest(
        @NotNull(message = "Le créneau est obligatoire")
        UUID horairePublicId
) {
}
