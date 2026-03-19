package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;

import java.util.List;
import java.util.UUID;

public interface HoraireService {

    HoraireResponse create(HoraireRequest request);

    HoraireResponse update(UUID publicId, HoraireRequest request);

    HoraireResponse getByPublicId(UUID publicId);

    List<HoraireResponse> getAll();

    List<HoraireResponse> getByParoisse(UUID paroissePublicId);

    List<HoraireResponse> getActiveByParoisse(UUID paroissePublicId);

    void deleteByPublicId(UUID publicId);
}