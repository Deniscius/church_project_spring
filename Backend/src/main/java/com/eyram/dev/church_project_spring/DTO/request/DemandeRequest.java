package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record DemandeRequest(

        @NotBlank(message = "L'intention est obligatoire")
        String intention,

        @NotBlank(message = "Le nom du fidèle est obligatoire")
        String nomFidele,

        @NotBlank(message = "Le prénom du fidèle est obligatoire")
        String prenomFidele,

        @NotBlank(message = "Le téléphone du fidèle est obligatoire")
        String telFidele,

        String emailFidele,

        String nomCoursier,

        LocalTime heurePersonnalisee,

        @NotNull(message = "La paroisse est obligatoire")
        UUID paroissePublicId,

        @NotNull(message = "Le type de demande est obligatoire")
        UUID typeDemandePublicId,

        @NotNull(message = "Le forfait tarifaire est obligatoire")
        UUID forfaitTarifPublicId,

        @NotNull(message = "Le type de paiement est obligatoire")
        UUID typePaiementPublicId,

        UUID horairePublicId,

        UUID userPublicId
) {
}