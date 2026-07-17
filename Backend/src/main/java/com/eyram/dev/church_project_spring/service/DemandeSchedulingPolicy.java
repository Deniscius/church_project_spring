package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void validateAllowedDay(LocalDate celebrationDate, Set<JourSemaine> allowedDays) {
        validateAllowedDay(celebrationDate, allowedDays, "Ce forfait");
    }

    public void validateAllowedDay(LocalDate celebrationDate, Set<JourSemaine> allowedDays, String contextLabel) {
        if (celebrationDate == null) {
            throw new BusinessRuleException("La date de célébration est obligatoire");
        }
        if (allowedDays == null || allowedDays.isEmpty()) {
            return;
        }

        JourSemaine requestedDay = JourSemaine.fromDayOfWeek(celebrationDate.getDayOfWeek());
        if (!allowedDays.contains(requestedDay)) {
            throw new BusinessRuleException(
                    contextLabel + " n'autorise pas de célébration le "
                            + requestedDay.getLibelle()
                            + ". Jours autorisés : "
                            + formatAllowedDays(allowedDays)
            );
        }
    }

    public Set<JourSemaine> resolveAllowedDays(Set<JourSemaine> typeDays, Set<JourSemaine> forfaitDays) {
        if (forfaitDays != null && !forfaitDays.isEmpty()) {
            if (typeDays != null && !typeDays.isEmpty()) {
                Set<JourSemaine> intersection = EnumSet.copyOf(forfaitDays);
                intersection.retainAll(typeDays);
                if (!intersection.isEmpty()) {
                    return intersection;
                }
            }
            return EnumSet.copyOf(forfaitDays);
        }
        if (typeDays != null && !typeDays.isEmpty()) {
            return EnumSet.copyOf(typeDays);
        }
        return EnumSet.allOf(JourSemaine.class);
    }

    public void validateHoraireDay(LocalDate celebrationDate, JourSemaine horaireDay) {
        if (celebrationDate == null || horaireDay == null) {
            return;
        }

        JourSemaine requestedDay = JourSemaine.fromDayOfWeek(celebrationDate.getDayOfWeek());
        if (!requestedDay.equals(horaireDay)) {
            throw new BusinessRuleException(
                    "La date choisie doit correspondre au jour de l'horaire sélectionné ("
                            + horaireDay.getLibelle()
                            + ")"
            );
        }
    }

    public List<LocalDate> computeCelebrationDates(
            LocalDate startDate,
            Set<JourSemaine> allowedDays,
            int nombreCelebrations
    ) {
        if (startDate == null) {
            throw new BusinessRuleException("La date de début est obligatoire");
        }
        if (nombreCelebrations <= 0) {
            throw new BusinessRuleException("Le nombre de célébrations doit être positif");
        }

        Set<JourSemaine> effectiveAllowedDays = allowedDays == null || allowedDays.isEmpty()
                ? EnumSet.allOf(JourSemaine.class)
                : allowedDays;

        validateAllowedDay(startDate, effectiveAllowedDays);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (dates.size() < nombreCelebrations) {
            JourSemaine currentDay = JourSemaine.fromDayOfWeek(current.getDayOfWeek());
            if (effectiveAllowedDays.contains(currentDay)) {
                dates.add(current);
            }
            current = current.plusDays(1);

            if (current.isAfter(startDate.plusYears(2))) {
                throw new BusinessRuleException(
                        "Impossible de planifier " + nombreCelebrations
                                + " célébration(s) avec les jours autorisés"
                );
            }
        }

        return dates;
    }

    private String formatAllowedDays(Set<JourSemaine> allowedDays) {
        return allowedDays.stream()
                .sorted(Comparator.comparingInt(JourSemaine::getOrdre))
                .map(JourSemaine::getLibelle)
                .collect(Collectors.joining(", "));
    }
}
