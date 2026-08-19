package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.DemandeListItemResponse;
import com.eyram.dev.church_project_spring.DTO.response.PageResponse;

import java.util.UUID;

public interface DemandeAdminSearchService {

    /**
     * Recherche paginée dans les demandes d'une paroisse par code, fidèle ou téléphone.
     */
    PageResponse<DemandeListItemResponse> search(
            UUID paroissePublicId,
            String query,
            int page,
            int size,
            boolean includeDeleted
    );
}
