package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireRequest(

        /**
         * Obligatoire pour un créneau hebdomadaire.
         * Optionnel si {@code dateSpecifique} est fourni (dérivé du jour de la date).
         */
        JourSemaine jourSemaine,

        @NotNull(message = "L'heure de célébration est obligatoire")
        LocalTime heureCelebration,

        @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères")
        String libelle,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean isActive,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        /** Date précise : créneau ponctuel (hors grille hebdomadaire). */
        LocalDate dateSpecifique,

        /**
         * Messe unique sur la paroisse pour {@code dateSpecifique}.
         * Ignoré / refusé si aucune date précise n'est fournie.
         */
        Boolean uniqueSurParoisse,

        /**
         * Honoraire imposé pour une date précise (événement solennel).
         * Obligatoire si {@code dateSpecifique} est renseigné ; ignoré sinon.
         */
        NatureForfaitEnum natureHonoraire
) {
}
