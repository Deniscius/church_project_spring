package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.enums.StatutDemandeEnum;

import java.util.List;
import java.util.UUID;

public interface DemandeService {

    DemandeResponse create(DemandeRequest request);

    DemandeResponse update(UUID publicId, DemandeRequest request);

    DemandeResponse getByPublicId(UUID publicId);

    DemandeResponse getByCodeSuivie(String codeSuivie);

    List<DemandeResponse> getAll();

    List<DemandeResponse> getByParoisse(UUID paroissePublicId);

    List<DemandeResponse> getByParoisseAndStatut(UUID paroissePublicId, StatutDemandeEnum statutDemande);

    void deleteByPublicId(UUID publicId);
}