package com.eyram.dev.church_project_spring.DTO.request;

import java.time.LocalDate;

public record DetailsPaiementRequest (
        LocalDate deteDetailsPaiement,
        Double montant

) {
}
