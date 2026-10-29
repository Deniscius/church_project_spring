package com.eyram.dev.church_project_spring.DTO.response;

import java.util.UUID;

public record ChoraleClaimResponse(
        UUID choralePublicId,
        String choraleNom,
        UUID paroissePublicId,
        String paroisseNom,
        UUID doyennePublicId,
        String doyenneNom
) {}
