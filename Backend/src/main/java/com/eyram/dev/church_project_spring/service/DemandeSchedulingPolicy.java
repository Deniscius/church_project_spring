package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class DemandeSchedulingPolicy {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final Clock clock;

    public void validate(LocalDate celebrationDate, LocalTime celebrationTime, Integer minimumLeadHours) {
        if (celebrationDate == null) {
            throw new BusinessRuleException("La date de célébration est obligatoire");
        }
        if (celebrationTime == null) {
            throw new BusinessRuleException("L'heure de célébration est obligatoire");
        }

        int leadHours = minimumLeadHours == null ? 24 : minimumLeadHours;
        LocalDateTime earliestAllowed = LocalDateTime.now(clock).plusHours(leadHours);
        LocalDateTime requested = LocalDateTime.of(celebrationDate, celebrationTime);

        if (requested.isBefore(earliestAllowed)) {
            throw new BusinessRuleException(
                    "Cette demande doit être déposée au moins " + leadHours
                            + " heure(s) avant la célébration. Choisissez une date et une heure à partir du "
                            + earliestAllowed.format(DEADLINE_FORMAT) + "."
            );
        }
    }
}
