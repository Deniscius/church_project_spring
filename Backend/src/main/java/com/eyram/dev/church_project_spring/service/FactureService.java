package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.FactureRequest;
import com.eyram.dev.church_project_spring.DTO.response.FactureResponse;

import java.util.List;
import java.util.UUID;

public interface FactureService {

    FactureResponse create(FactureRequest request);

    FactureResponse getByPublicId(UUID publicId);

    FactureResponse getByCodeSuivie(String codeSuivie);

    List<FactureResponse> getAll();

    FactureResponse update(UUID publicId, FactureRequest request);

    void delete(UUID publicId);
}