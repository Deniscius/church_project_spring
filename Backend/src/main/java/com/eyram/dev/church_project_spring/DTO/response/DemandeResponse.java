package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.DureeMesseEnum;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DemandeResponse(
        UUID publicId,
        String intention,
        LocalDate dateDemande,
        StatusValidationEnum statusValidationEnum,
        Boolean statusDel,
        TypeDemandeEnum typeDemandeEnum,
        DureeMesseEnum dureeMesse,
        List<LocalDate> dates,
        Integer prixTotal,
        FideleResponse fidele,
        TypeDemandeResponse typeDemande
) {}
