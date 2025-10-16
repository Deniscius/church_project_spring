package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.util.UUID;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;

public record DemandeResponse(
        UUID publicId,
        String intention,
        LocalDate dateDemande,
        StatusValidationEnum statusValidationEnum,
        Boolean statusDel,
        FideleResponse client,
        TypeDemandeResponse typeDemande
) {}