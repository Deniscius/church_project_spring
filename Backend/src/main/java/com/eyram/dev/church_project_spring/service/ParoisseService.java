package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseResponse;

import java.util.List;
import java.util.UUID;

public interface ParoisseService {

    ParoisseResponse create(ParoisseRequest request);
    List<ParoisseResponse> getAll();
    ParoisseResponse getByPublicId(UUID publicId);
    ParoisseResponse update(UUID publicId, ParoisseRequest request);
    void deleteByPublicId(UUID publicId);
}