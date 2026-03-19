package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;

import java.util.List;
import java.util.UUID;

public interface TypePaiementService {

    TypePaiementResponse create(TypePaiementRequest request);

    TypePaiementResponse getByPublicId(UUID publicId);

    List<TypePaiementResponse> getAll();

    TypePaiementResponse update(UUID publicId, TypePaiementRequest request);

    void delete(UUID publicId);
}