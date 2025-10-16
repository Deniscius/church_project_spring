package com.eyram.dev.church_project_spring.DTO.response;

import com.eyram.dev.church_project_spring.enums.JourSemaineEnum;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record HoraireResponse(
        UUID publicID,
        JourSemaineEnum jourSemaine,
        LocalTime horaire,
        Boolean statusDel
) {
}
