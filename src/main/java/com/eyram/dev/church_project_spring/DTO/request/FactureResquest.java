package com.eyram.dev.church_project_spring.DTO.request;

import java.time.LocalDate;

public record FactureResquest(
        LocalDate datePaiement,
        Double montant,
        LocalDate dateProduction
) {
}
