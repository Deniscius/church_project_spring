package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.LocaliteRequest;
import com.eyram.dev.church_project_spring.DTO.response.LocaliteResponse;

import java.util.List;
import java.util.UUID;

public interface LocaliteService {

    LocaliteResponse create(LocaliteRequest request);
    LocaliteResponse getByPublicId(UUID publicId);
    void delete(UUID publicId);
    LocaliteResponse update(UUID publicId, LocaliteRequest request);
    List<LocaliteResponse> getAll();


}
