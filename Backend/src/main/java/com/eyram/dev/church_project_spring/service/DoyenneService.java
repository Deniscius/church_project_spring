package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DoyenneRequest;
import com.eyram.dev.church_project_spring.DTO.response.DoyenneResponse;

import java.util.List;
import java.util.UUID;

public interface DoyenneService {

    DoyenneResponse create(DoyenneRequest request);
    DoyenneResponse getByPublicId(UUID publicId);
    List<DoyenneResponse> getAll();
    DoyenneResponse update(UUID publicId, DoyenneRequest request);
    void deleteByPublicId(UUID publicId);
}