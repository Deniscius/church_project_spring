package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.MandatPastoralRequest;
import com.eyram.dev.church_project_spring.DTO.response.MandatPastoralResponse;

import java.util.List;
import java.util.UUID;

public interface MandatPastoralService {

    MandatPastoralResponse create(MandatPastoralRequest request);

    MandatPastoralResponse update(UUID publicId, MandatPastoralRequest request);

    MandatPastoralResponse getByPublicId(UUID publicId);

    List<MandatPastoralResponse> getByAnnee(UUID paroissePublicId, UUID anneePastoralePublicId);

    void archive(UUID publicId, Long version);
}
