package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAccessRequest;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.mappers.ParoisseAccessMapper;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParoisseAccessServiceImplTest {

    @Mock private ParoisseAccessRepository accessRepository;
    @Mock private UserRepository userRepository;
    @Mock private ParoisseRepository paroisseRepository;
    @Mock private ParoisseAccessMapper mapper;
    @InjectMocks private ParoisseAccessServiceImpl service;

    @Test
    void refusesParishAccessForGlobalUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, true, true);
        ParoisseAccessRequest request = request(userId, UUID.randomUUID(), true);
        when(userRepository.findByPublicIdAndStatusDelFalse(userId)).thenReturn(Optional.of(user));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.create(request)
        );

        assertEquals(
                "Un utilisateur global ne doit pas être associé à une paroisse",
                exception.getMessage()
        );
        verify(paroisseRepository, never()).findByPublicIdAndStatusDelFalse(any());
    }

    @Test
    void refusesASecondActiveParish() {
        UUID userId = UUID.randomUUID();
        UUID paroisseId = UUID.randomUUID();
        User user = user(userId, false, true);
        Paroisse paroisse = activeParoisse(paroisseId);
        ParoisseAccess existing = access(UUID.randomUUID(), user, paroisse, true);

        when(userRepository.findByPublicIdAndStatusDelFalse(userId)).thenReturn(Optional.of(user));
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisseId))
                .thenReturn(Optional.of(paroisse));
        when(accessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of(existing));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.create(request(userId, paroisseId, true))
        );

        assertEquals(
                "Un utilisateur local ne peut avoir qu'une seule paroisse active",
                exception.getMessage()
        );
        verify(accessRepository, never()).save(any());
    }

    @Test
    void refusesDeletingTheActiveAccessOfAnActiveUser() {
        UUID accessId = UUID.randomUUID();
        User user = user(UUID.randomUUID(), false, true);
        ParoisseAccess access = access(accessId, user, activeParoisse(UUID.randomUUID()), true);
        when(accessRepository.findByPublicIdAndStatusDelFalse(accessId))
                .thenReturn(Optional.of(access));

        assertThrows(BusinessRuleException.class, () -> service.deleteByPublicId(accessId));

        assertFalse(access.getStatusDel());
        verify(accessRepository, never()).save(any());
    }

    private ParoisseAccessRequest request(UUID userId, UUID paroisseId, boolean active) {
        return new ParoisseAccessRequest(
                userId,
                paroisseId,
                RoleParoisse.SECRETAIRE,
                active
        );
    }

    private User user(UUID publicId, boolean global, boolean active) {
        User user = new User();
        user.setPublicId(publicId);
        user.setIsGlobal(global);
        user.setIsActive(active);
        user.setStatusDel(false);
        return user;
    }

    private Paroisse activeParoisse(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        return paroisse;
    }

    private ParoisseAccess access(
            UUID publicId,
            User user,
            Paroisse paroisse,
            boolean active
    ) {
        ParoisseAccess access = new ParoisseAccess();
        access.setPublicId(publicId);
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(RoleParoisse.SECRETAIRE);
        access.setActive(active);
        access.setStatusDel(false);
        return access;
    }
}
