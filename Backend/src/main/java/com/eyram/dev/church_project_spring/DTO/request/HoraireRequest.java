package com.eyram.dev.church_project_spring.DTO.request;
import com.eyram.dev.church_project_spring.enums.JourSemaine;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireRequest(

        @NotNull(message = "Le jour de la semaine est obligatoire")
        JourSemaine jourSemaine,

        @NotNull(message = "L'heure de célébration est obligatoire")
        LocalTime heureCelebration,

        @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
        String libelle,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean isActive,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId

) {
}
