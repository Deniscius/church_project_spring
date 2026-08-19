package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Remplacement complet des créneaux disponibles pour une date précise.
 *
 * Les demandes déjà enregistrées ne sont pas déplacées automatiquement :
 * l'administrateur conserve la main sur leur créneau individuel.
 */
public record ProgrammeJourUpdateRequest(
        @NotEmpty(message = "Au moins un créneau est obligatoire")
        @Size(max = 20, message = "Une journée ne peut pas contenir plus de 20 créneaux")
        List<@Valid Creneau> creneaux
) {
    public record Creneau(
            @NotNull(message = "L'heure de célébration est obligatoire")
            LocalTime heureCelebration,

            @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
            String libelle,

            /** Optionnel : par défaut DOMINICALE le dimanche, NORMALE sinon. */
            NatureForfaitEnum natureHonoraire
    ) {
    }
}
