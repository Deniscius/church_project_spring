package com.eyram.dev.church_project_spring.service.audit;

import com.eyram.dev.church_project_spring.DTO.response.AdministrativeAuditEventResponse;
import com.eyram.dev.church_project_spring.entities.AdministrativeAuditEvent;
import com.eyram.dev.church_project_spring.repositories.AdministrativeAuditEventRepository;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdministrativeAuditService {

    public static final String REGISTRATION_APPROVED = "PARISH_REGISTRATION_APPROVED";
    public static final String REGISTRATION_REJECTED = "PARISH_REGISTRATION_REJECTED";
    public static final String SUBSCRIPTION_MANUALLY_ACTIVATED = "SUBSCRIPTION_MANUALLY_ACTIVATED";
    public static final String SUBSCRIPTION_EXTENDED = "SUBSCRIPTION_EXTENDED";
    public static final String SUBSCRIPTION_PENDING_CANCELLED = "SUBSCRIPTION_PENDING_CANCELLED";
    public static final String SUBSCRIPTION_TERMINATED = "SUBSCRIPTION_TERMINATED";

    private final AdministrativeAuditEventRepository repository;

    public void record(
            String action,
            String targetType,
            UUID targetPublicId,
            UUID paroissePublicId,
            String details
    ) {
        AdministrativeAuditEvent event = new AdministrativeAuditEvent();
        event.setAction(requireText(action, "Action d'audit requise", 80));
        event.setTargetType(requireText(targetType, "Type de cible requis", 60));
        if (targetPublicId == null) {
            throw new IllegalArgumentException("Identifiant de cible requis");
        }
        event.setTargetPublicId(targetPublicId);
        event.setParoissePublicId(paroissePublicId);
        event.setDetails(clean(details, 500));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof UserDetailsImpl user) {
            event.setActorPublicId(user.getPublicId());
            event.setActorUsername(cleanOrDefault(user.getUsername(), "unknown", 100));
            event.setActorName(cleanOrDefault(user.getFullName(), user.getUsername(), 160));
        } else {
            event.setActorUsername("SYSTEM");
            event.setActorName("Système");
        }

        repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AdministrativeAuditEventResponse> listRecent() {
        return repository.findTop200ByOrderByOccurredAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AdministrativeAuditEventResponse toResponse(AdministrativeAuditEvent event) {
        return new AdministrativeAuditEventResponse(
                event.getPublicId(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetPublicId(),
                event.getParoissePublicId(),
                event.getActorPublicId(),
                event.getActorUsername(),
                event.getActorName(),
                event.getDetails(),
                event.getOccurredAt()
        );
    }

    private static String requireText(String value, String message, int maxLength) {
        String cleaned = clean(value, maxLength);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private static String cleanOrDefault(String value, String fallback, int maxLength) {
        String cleaned = clean(value, maxLength);
        return StringUtils.hasText(cleaned) ? cleaned : clean(fallback, maxLength);
    }

    private static String clean(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\r\\n\\t]+", " ");
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
