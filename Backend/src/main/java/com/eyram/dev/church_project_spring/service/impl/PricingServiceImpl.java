package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.enums.DureeMesseEnum;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;
import com.eyram.dev.church_project_spring.service.PricingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PricingServiceImpl implements PricingService {

    @Override
    public int priceFor(TypeDemandeEnum type, LocalDate date) {
        Objects.requireNonNull(type, "type requis");
        Objects.requireNonNull(date, "date requise");
        return type.getPrixPourJour(date.getDayOfWeek());
    }

    @Override
    public int totalFor(TypeDemandeEnum type, DureeMesseEnum duree, List<LocalDate> dates) {
        Objects.requireNonNull(type, "type requis");
        Objects.requireNonNull(duree, "durée requise");

        if (duree.isPack()) {
            if (dates == null || dates.size() < duree.getNbJours()) {
                throw new IllegalArgumentException("Nombre de dates insuffisant pour " + duree);
            }
            return duree.getPrixTotal();
        }

        if (dates == null || dates.isEmpty()) {
            throw new IllegalArgumentException("Au moins une date est requise pour SIMPLE");
        }

        int total = 0;
        for (LocalDate d : dates) {
            total += priceFor(type, d);
        }
        return total;
    }
}
