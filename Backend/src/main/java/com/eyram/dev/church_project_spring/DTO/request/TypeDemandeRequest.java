package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record TypeDemandeRequest(

        @NotBlank(message = "Le nom du type de demande est obligatoire")
        @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
        String libelle,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
        String description,

        @NotNull(message = "Le type de demande est obligatoire")
        TypeDemandeEnum typeDemandeEnum,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le délai minimum est obligatoire")
        @Min(value = 0, message = "Le délai minimum ne peut pas être négatif")
        @Max(value = 8760, message = "Le délai minimum ne peut pas dépasser un an")
        Integer delaiMinimumHeures,

        @NotEmpty(message = "Sélectionnez au moins un jour de célébration autorisé")
        Set<JourSemaine> joursCelebrationAutorises,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId
) {
}
