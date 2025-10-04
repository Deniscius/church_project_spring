package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.TypePaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypePaiementResponse;

import java.util.List;
import java.util.UUID;

public interface TypePaiementService {
    TypePaiementResponse create(TypePaiementRequest typePaiementRequest);
    TypePaiementResponse update(UUID publicId, TypePaiementRequest typePaiementRequest);
    List<TypePaiementResponse> findAll();
    TypePaiementResponse findById(UUID publicId);
    void delete(UUID publicId);
}
