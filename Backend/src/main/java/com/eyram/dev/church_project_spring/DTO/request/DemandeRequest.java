package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DemandeRequest(

        @NotNull(message = "L'intention est obligatoire")
        String intention,

        @NotNull(message = "Le nom du fidèle est obligatoire")
        String nomFidele,

        @NotNull(message = "Le prénom du fidèle est obligatoire")
        String prenomFidele,

        @NotNull(message = "Le téléphone du fidèle est obligatoire")
        String telFidele,

        String emailFidele,
        String nomCoursier,
        LocalTime heurePersonnalisee,

        LocalDate dateDebut,

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