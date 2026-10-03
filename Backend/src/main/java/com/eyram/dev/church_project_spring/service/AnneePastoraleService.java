package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.AnneePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.AnneePastoraleResponse;

import java.util.List;
import java.util.UUID;

public interface AnneePastoraleService {

    AnneePastoraleResponse create(AnneePastoraleRequest request);

    AnneePastoraleResponse update(UUID publicId, AnneePastoraleRequest request);

    AnneePastoraleResponse getByPublicId(UUID publicId);

    List<AnneePastoraleResponse> getByParoisse(UUID paroissePublicId);

    AnneePastoraleResponse getPublishedByParoisse(UUID paroissePublicId);

    AnneePastoraleResponse publish(UUID publicId, Long version);

    AnneePastoraleResponse close(UUID publicId, Long version);

    void deleteByPublicId(UUID publicId, Long version);
}
