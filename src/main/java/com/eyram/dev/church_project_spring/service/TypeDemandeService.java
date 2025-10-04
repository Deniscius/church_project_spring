package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;

import java.util.List;
import java.util.UUID;

public interface TypeDemandeService {

    TypeDemandeResponse create(TypeDemandeRequest request);
    TypeDemandeResponse update(UUID publicId, TypeDemandeRequest request);
    void delete(UUID publicId);
    TypeDemandeResponse getById(UUID publicId);
    List<TypeDemandeResponse> getAll();
    List<TypeDemandeResponse> getAllForAdmin();




}
