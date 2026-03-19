package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DemandeDateRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeDateResponse;

import java.util.List;
import java.util.UUID;

public interface DemandeDateService {

    DemandeDateResponse create(DemandeDateRequest request);

    DemandeDateResponse update(UUID publicId, DemandeDateRequest request);

    DemandeDateResponse getByPublicId(UUID publicId);

    List<DemandeDateResponse> getAll();

    List<DemandeDateResponse> getByDemande(UUID demandePublicId);

    void deleteByPublicId(UUID publicId);
}