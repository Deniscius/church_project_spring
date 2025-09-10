package com.eyram.dev.church_project_spring.DTO.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireResponse(
        UUID publicID,
        DayOfWeek jourSemaine,
        LocalTime horaire,
        Boolean statusDel
) {
}
