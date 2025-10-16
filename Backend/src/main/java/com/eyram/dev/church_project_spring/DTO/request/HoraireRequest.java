package com.eyram.dev.church_project_spring.DTO.request;

import com.eyram.dev.church_project_spring.enums.JourSemaineEnum;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HoraireRequest (
        JourSemaineEnum jourSemaine,
        LocalTime heure
) {

}
