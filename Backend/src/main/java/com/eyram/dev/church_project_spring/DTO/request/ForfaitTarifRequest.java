package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ForfaitTarifRequest(

        String codeForfait,

        @NotBlank(message = "Le nom du forfait est obligatoire")
        String nomForfait,

        @NotNull(message = "La nature du forfait est obligatoire")
        NatureForfaitEnum natureForfait,

        @NotNull(message = "Le montant du forfait est obligatoire")
        @Positive(message = "Le montant du forfait doit être supérieur à zéro")
        BigDecimal montantForfait,

        Integer nombreJour,

        Integer nombreCelebration,

        @NotEmpty(message = "Au moins un jour de célébration est obligatoire pour le forfait")
        Set<JourSemaine> joursCelebrationAutorises,

        @NotNull(message = "Le champ heure personnalisée est obligatoire")
        Boolean heurePersonnalise,

        String libelle,

        @NotNull(message = "L'état d'activation est obligatoire")
        Boolean isActive,

        @NotNull(message = "Le type de demande est obligatoire")
        UUID typeDemandePublicId
) {
}