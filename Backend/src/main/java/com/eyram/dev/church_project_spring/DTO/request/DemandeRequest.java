package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.DureeMesseEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DemandeRequest(
        String intention,
        LocalDate dateDemande,
        UUID fidelePublicId,
        UUID typeDemandePublicId,    // seulement si tu gardes l'entité TypeDemande
        TypeDemandeEnum typeDemandeEnum,  // <- nouvel enum
        DureeMesseEnum dureeMesse,        // <- durée de la série
        List<LocalDate> dates             // <- liste des jours (successifs ou non)
) {}
