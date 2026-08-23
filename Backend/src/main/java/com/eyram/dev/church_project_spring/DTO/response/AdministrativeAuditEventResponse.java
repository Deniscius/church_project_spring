package com.eyram.dev.church_project_spring.DTO.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdministrativeAuditEventResponse(
        UUID publicId,
        String action,
        String targetType,
        UUID targetPublicId,
        UUID paroissePublicId,
        UUID actorPublicId,
        String actorUsername,
        String actorName,
        String details,
        LocalDateTime occurredAt
) {
}
