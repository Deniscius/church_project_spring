package com.eyram.dev.church_project_spring.DTO.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HoraireRequest (
        DayOfWeek jourSemaine,
        LocalTime heure
) {

}
