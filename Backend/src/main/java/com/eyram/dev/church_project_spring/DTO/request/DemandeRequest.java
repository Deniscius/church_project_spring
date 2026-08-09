package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DemandeRequest(

        @NotBlank(message = "L'intention est obligatoire")
        @Size(min = 10, max = 500, message = "L'intention doit contenir entre 10 et 500 caractères")
        String intention,

        /** Optionnel : avec prénom vide → défaut « Un(e) chrétien(ne) » si les deux absents. */
        @Size(max = 100, message = "Le nom du fidèle ne peut pas dépasser 100 caractères")
        String nomFidele,

        /** Optionnel : avec nom vide → défaut « Un(e) chrétien(ne) » si les deux absents. */
        @Size(max = 100, message = "Le prénom du fidèle ne peut pas dépasser 100 caractères")
        String prenomFidele,

        @NotBlank(message = "Le téléphone du fidèle est obligatoire")
        @Size(max = 30, message = "Le téléphone du fidèle ne peut pas dépasser 30 caractères")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Le téléphone doit être au format international E.164 (ex. +22890123456)"
        )
        String telFidele,

        @Email(message = "L'adresse email du fidèle est invalide")
        @Size(max = 150, message = "L'adresse email du fidèle ne peut pas dépasser 150 caractères")
        String emailFidele,

        @Size(max = 100, message = "Le nom du coursier ne peut pas dépasser 100 caractères")
        String nomCoursier,

        LocalTime heurePersonnalisee,

        /**
         * Première célébration. Obligatoire pour un forfait à 1 célébration ;
         * optionnel si {@code datesCelebration} est fourni (dérivé du minimum).
         */
        LocalDate dateDebut,

        /**
         * Dates de célébration pour les forfaits multi-jours (triduum / neuvaine / trentaine).
         * Optionnel si {@code dateDebut} ou {@code celebrationSlots} est fourni.
         */
        @Size(max = 30, message = "Au maximum 30 dates de célébration peuvent être fournies")
        List<LocalDate> datesCelebration,

        /**
         * Créneaux détaillés (date + horaire du jour). Prioritaire sur {@code datesCelebration}
         * pour les multi-célébrations.
         */
        @Size(max = 30, message = "Au maximum 30 créneaux de célébration peuvent être fournis")
        List<CelebrationSlotRequest> celebrationSlots,

        /**
         * {@code true} = jours calendaires consécutifs ; {@code false} = dates libres
         * dans la fenêtre {@code nombreJour}. {@code null} = déduit des dates.
         */
        Boolean joursConsecutifs,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @NotNull(message = "Le type de demande est obligatoire")
        UUID typeDemandePublicId,

        @NotNull(message = "Le forfait tarifaire est obligatoire")
        UUID forfaitTarifPublicId,

        UUID horairePublicId,
        UUID userPublicId,

        @NotNull(message = "Le type de paiement est obligatoire")
        UUID typePaiementPublicId
) {
}
