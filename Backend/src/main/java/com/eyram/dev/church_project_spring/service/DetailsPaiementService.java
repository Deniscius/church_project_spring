package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DetailsPaiementRequest;
import com.eyram.dev.church_project_spring.DTO.response.DetailsPaiementResponse;

import java.util.List;
import java.util.UUID;

public interface DetailsPaiementService {

    DetailsPaiementResponse create(DetailsPaiementRequest request);

    DetailsPaiementResponse getByPublicId(UUID publicId);

    List<DetailsPaiementResponse> getAll();

    DetailsPaiementResponse update(UUID publicId, DetailsPaiementRequest request);

    void delete(UUID publicId);
}