package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ForfaitTarifRequest;
import com.eyram.dev.church_project_spring.DTO.response.ForfaitTarifResponse;

import java.util.List;
import java.util.UUID;

public interface ForfaitTarifService {

    ForfaitTarifResponse create(ForfaitTarifRequest request);

    ForfaitTarifResponse update(UUID publicId, ForfaitTarifRequest request);

    ForfaitTarifResponse getByPublicId(UUID publicId);

    List<ForfaitTarifResponse> getAll();

    List<ForfaitTarifResponse> getByTypeDemande(UUID typeDemandePublicId);

    List<ForfaitTarifResponse> getActiveByTypeDemande(UUID typeDemandePublicId);

    void deleteByPublicId(UUID publicId);
}