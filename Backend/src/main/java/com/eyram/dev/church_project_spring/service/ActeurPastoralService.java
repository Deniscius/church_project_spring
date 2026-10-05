package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ActeurPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.ActeurPastoralResponse;

import java.util.List;
import java.util.UUID;

public interface ActeurPastoralService {

    ActeurPastoralResponse create(ActeurPastoralRequest request);

    ActeurPastoralResponse update(UUID publicId, ActeurPastoralRequest request);

    ActeurPastoralResponse getByPublicId(UUID publicId);

    List<ActeurPastoralResponse> getByParoisse(UUID paroissePublicId, Boolean actif, String recherche);

    ActeurPastoralResponse setActif(UUID publicId, Boolean actif, Long version);

    void archive(UUID publicId, Long version);
}
