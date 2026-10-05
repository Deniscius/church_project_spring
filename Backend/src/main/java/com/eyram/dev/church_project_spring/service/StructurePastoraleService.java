package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.StructurePastoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.StructurePastoraleResponse;

import java.util.List;
import java.util.UUID;

public interface StructurePastoraleService {

    StructurePastoraleResponse create(StructurePastoraleRequest request);

    StructurePastoraleResponse update(UUID publicId, StructurePastoraleRequest request);

    StructurePastoraleResponse getByPublicId(UUID publicId);

    List<StructurePastoraleResponse> getByParoisse(UUID paroissePublicId);

    StructurePastoraleResponse setActif(UUID publicId, Boolean actif, Long version);

    void archive(UUID publicId, Long version);
}
