package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DemandeRequest(

        @NotBlank(message = "L'intention est obligatoire")
        @Size(max = 500, message = "L'intention ne peut pas dépasser 500 caractères")
        String intention,

        @NotBlank(message = "Le nom du fidèle est obligatoire")
        @Size(max = 100, message = "Le nom du fidèle ne peut pas dépasser 100 caractères")
        String nomFidele,

        @NotBlank(message = "Le prénom du fidèle est obligatoire")
        @Size(max = 100, message = "Le prénom du fidèle ne peut pas dépasser 100 caractères")
        String prenomFidele,

        @NotBlank(message = "Le téléphone du fidèle est obligatoire")
        @Size(max = 30, message = "Le téléphone du fidèle ne peut pas dépasser 30 caractères")
        String telFidele,

        @Email(message = "L'adresse email du fidèle est invalide")
        @Size(max = 150, message = "L'adresse email du fidèle ne peut pas dépasser 150 caractères")
        String emailFidele,

        @Size(max = 100, message = "Le nom du coursier ne peut pas dépasser 100 caractères")
        String nomCoursier,

        LocalTime heurePersonnalisee,

        @NotNull(message = "La date de début est obligatoire")
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
