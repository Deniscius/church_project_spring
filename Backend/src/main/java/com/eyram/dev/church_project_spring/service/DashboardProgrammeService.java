package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DashboardProgrammeService {

    /**
     * Retourne les célébrations réellement programmées pour la paroisse sur les
     * prochains jours. La fenêtre inclut aujourd'hui.
     */
    List<UpcomingCelebrationResponse> findUpcoming(UUID paroissePublicId, int jours);

    /** Historique récent des célébrations dont l'heure est déjà passée. */
    List<UpcomingCelebrationResponse> findPast(UUID paroissePublicId, int jours);

    /** Toutes les programmations réelles d'une journée précise. */
    List<UpcomingCelebrationResponse> findByDate(UUID paroissePublicId, LocalDate date);

    /**
     * Change uniquement le créneau sur la journée déjà choisie.
     * La date reste inchangée afin de ne pas contourner les règles tarifaires.
     */
    UpcomingCelebrationResponse updateSchedule(
            UUID paroissePublicId,
            UUID demandeDatePublicId,
            UUID horairePublicId
    );
}
