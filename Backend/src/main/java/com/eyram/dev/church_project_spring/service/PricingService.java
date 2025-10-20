package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.enums.DureeMesseEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.time.LocalDate;
import java.util.List;

public interface PricingService {
    int priceFor(TypeDemandeEnum type, LocalDate date);
    int totalFor(TypeDemandeEnum type, DureeMesseEnum duree, List<LocalDate> dates);}
