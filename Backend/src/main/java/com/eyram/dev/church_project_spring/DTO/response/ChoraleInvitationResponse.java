package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChoraleInvitationResponse(UUID publicId, String token, LocalDateTime expiresAt) {}
