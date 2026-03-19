package com.eyram.dev.church_project_spring.DTO.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record DemandeDateRequest(

        @NotNull(message = "L'ordre est obligatoire")
        @Positive(message = "L'ordre doit être supérieur à zéro")
        Integer ordre,

        @NotNull(message = "La date de célébration est obligatoire")
        LocalDate dateCelebration,

        @NotNull(message = "La demande est obligatoire")
        UUID demandePublicId
) {
}