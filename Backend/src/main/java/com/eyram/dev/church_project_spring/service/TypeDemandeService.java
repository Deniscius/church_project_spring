package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.TypeDemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.TypeDemandeResponse;
import com.eyram.dev.church_project_spring.enums.TypeDemandeEnum;

import java.util.List;
import java.util.UUID;

public interface TypeDemandeService {

    TypeDemandeResponse create(TypeDemandeRequest request);

    TypeDemandeResponse update(UUID publicId, TypeDemandeRequest request);

    TypeDemandeResponse getByPublicId(UUID publicId);

    List<TypeDemandeResponse> getAll();

    List<TypeDemandeResponse> getByParoisse(UUID paroissePublicId);

    List<TypeDemandeResponse> getByTypeDemandeEnum(TypeDemandeEnum typeDemandeEnum);

    List<TypeDemandeResponse> getByParoisseAndTypeDemandeEnum(UUID paroissePublicId, TypeDemandeEnum typeDemandeEnum);

    void deleteByPublicId(UUID publicId);
}