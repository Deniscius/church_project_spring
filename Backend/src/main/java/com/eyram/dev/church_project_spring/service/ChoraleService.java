package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ChoraleRequest;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleClaimResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleInvitationResponse;
import com.eyram.dev.church_project_spring.DTO.response.ChoraleResponse;

import java.util.List;
import java.util.UUID;

public interface ChoraleService {
    ChoraleResponse create(UUID paroissePublicId, ChoraleRequest request);
    List<ChoraleResponse> list(UUID paroissePublicId);
    ChoraleResponse get(UUID publicId);
    ChoraleResponse update(UUID publicId, ChoraleRequest request);
    void delete(UUID publicId);
    ChoraleInvitationResponse invite(UUID publicId);
    ChoraleClaimResponse resolveInvitation(String token);
    ChoraleClaimResponse consumeInvitation(String token);
}
