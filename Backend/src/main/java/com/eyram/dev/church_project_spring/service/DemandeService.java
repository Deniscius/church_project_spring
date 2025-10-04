package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.DemandeRequest;
import com.eyram.dev.church_project_spring.DTO.response.DemandeResponse;
import com.eyram.dev.church_project_spring.enums.StatusValidationEnum;

import java.util.List;
import java.util.UUID;

public interface DemandeService {
    DemandeResponse create(DemandeRequest request);

    DemandeResponse getByPublicId(UUID publicId);
    List<DemandeResponse> getAll();
    List<DemandeResponse> getAllByClient(UUID clientPublicId);
    List<DemandeResponse> getAllValidated(boolean statusValidation);
    DemandeResponse update(UUID publicId, DemandeRequest request);
    DemandeResponse validateDemande(UUID publicId);
    DemandeResponse cancelDemande(UUID publicId);
    void delete(UUID publicId);
    List<DemandeResponse> getAllByStatus(StatusValidationEnum status);

}
