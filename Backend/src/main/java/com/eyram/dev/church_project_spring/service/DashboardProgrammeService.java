package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.UpcomingCelebrationResponse;

import java.util.List;
import java.util.UUID;

public interface DashboardProgrammeService {

    /**
     * Retourne les célébrations réellement programmées pour la paroisse sur les
     * prochains jours. La fenêtre inclut aujourd'hui.
     */
    List<UpcomingCelebrationResponse> findUpcoming(UUID paroissePublicId, int jours);
}
