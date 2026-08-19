package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.PlanSaasCreateRequest;
import com.eyram.dev.church_project_spring.DTO.request.PlanSaasRequest;
import com.eyram.dev.church_project_spring.DTO.response.PlanSaasResponse;
import com.eyram.dev.church_project_spring.entities.PlanSaas;

import java.util.List;
import java.util.UUID;

public interface PlanSaasService {

    List<PlanSaasResponse> findAll();

    List<PlanSaasResponse> findPublicActive();

    PlanSaasResponse create(PlanSaasCreateRequest request);

    PlanSaasResponse update(UUID publicId, PlanSaasRequest request);

    PlanSaas require(String code);

    PlanSaas requireActive(String code);

    PlanSaas requireDefaultActive();
}
