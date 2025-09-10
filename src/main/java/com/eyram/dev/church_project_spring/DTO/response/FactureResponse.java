package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDate;
import java.util.UUID;

public record FactureResponse(
        UUID publicId,
        LocalDate datePaiement,
        Double montant,
        LocalDate dateProduction,
        Boolean statusDel
) {
}
