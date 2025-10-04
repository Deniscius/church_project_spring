package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.HoraireRequest;
import com.eyram.dev.church_project_spring.DTO.response.HoraireResponse;
import com.eyram.dev.church_project_spring.DTO.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface HoraireService {
    HoraireResponse create (HoraireRequest horaireRequest);
    HoraireResponse update (UUID publicId, HoraireRequest horaireRequest);
    List<HoraireResponse> getAll();
    HoraireResponse getByPublicId(UUID publicId);
    void delete(UUID publicId);


}
