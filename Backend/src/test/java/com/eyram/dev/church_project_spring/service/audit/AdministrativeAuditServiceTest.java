package com.eyram.dev.church_project_spring.service.audit;

import com.eyram.dev.church_project_spring.entities.AdministrativeAuditEvent;
import com.eyram.dev.church_project_spring.repositories.AdministrativeAuditEventRepository;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdministrativeAuditServiceTest {

    private final AdministrativeAuditEventRepository repository =
            mock(AdministrativeAuditEventRepository.class);
    private final AdministrativeAuditService service = new AdministrativeAuditService(repository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordCapturesAuthenticatedActorAndSanitizesDetails() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID parishId = UUID.randomUUID();
        UserDetailsImpl principal = new UserDetailsImpl(
                actorId,
                "Alice Admin",
                "alice",
                null,
                true,
                0L,
                "ignored",
                Set.of(),
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Set.of())
        );

        service.record(
                AdministrativeAuditService.REGISTRATION_REJECTED,
                "PARISH_REGISTRATION",
                targetId,
                parishId,
                "Motif ligne 1\nligne 2"
        );

        ArgumentCaptor<AdministrativeAuditEvent> captor =
                ArgumentCaptor.forClass(AdministrativeAuditEvent.class);
        verify(repository).save(captor.capture());
        AdministrativeAuditEvent saved = captor.getValue();

        assertEquals(actorId, saved.getActorPublicId());
        assertEquals("alice", saved.getActorUsername());
        assertEquals("Alice Admin", saved.getActorName());
        assertEquals(targetId, saved.getTargetPublicId());
        assertEquals(parishId, saved.getParoissePublicId());
        assertEquals("Motif ligne 1 ligne 2", saved.getDetails());
        assertFalse(saved.getDetails().contains("\n"));
    }

    @Test
    void recordUsesSystemIdentityWithoutAuthentication() {
        service.record(
                AdministrativeAuditService.SUBSCRIPTION_TERMINATED,
                "PARISH",
                UUID.randomUUID(),
                null,
                null
        );

        ArgumentCaptor<AdministrativeAuditEvent> captor =
                ArgumentCaptor.forClass(AdministrativeAuditEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getActorUsername());
        assertEquals("Système", captor.getValue().getActorName());
    }
}
