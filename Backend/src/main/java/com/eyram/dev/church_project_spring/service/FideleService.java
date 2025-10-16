package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.FideleRequest;
import com.eyram.dev.church_project_spring.DTO.response.FideleResponse;

import java.util.List;
import java.util.UUID;

public interface FideleService {
    FideleResponse create(FideleRequest request);
    List<FideleResponse> getAll();
    List<FideleResponse> getActive();
    List<FideleResponse> getInactive();
    List<FideleResponse> getNom(String nom);
    FideleResponse getByPublicId(UUID publicId);
    FideleResponse update(UUID publicId, FideleRequest request);
    void softDelete(UUID publicId);
}
