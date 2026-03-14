package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.DTO.response.ParoisseAccessResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;

import java.util.List;
import java.util.UUID;

public interface ParoisseAccessService {

    ParoisseAccessResponse create(ParoisseAccessRequest request);

    ParoisseAccessResponse update(UUID publicId, ParoisseAccessRequest request);

    ParoisseAccessResponse getByPublicId(UUID publicId);

    List<ParoisseAccessResponse> getAll();

    List<ParoisseAccessResponse> getByUser(UUID userPublicId);

    List<ParoisseAccessResponse> getByParoisse(UUID paroissePublicId);

    void deleteByPublicId(UUID publicId);

    boolean hasAccessToParoisse(User user, Paroisse paroisse);
}