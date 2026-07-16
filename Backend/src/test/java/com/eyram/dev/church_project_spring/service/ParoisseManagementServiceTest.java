package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParoisseManagementServiceTest {

    @Mock private ParoisseRepository paroisseRepository;
    @Mock private EnhancedUserService userService;
    @InjectMocks private ParoisseManagementService service;

    @Test
    void createsLocalAdminWithOneParishAssignment() {
        UUID parishId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Paroisse paroisse = activeParish(parishId);
        UserRequest input = new UserRequest(
                "Dupont", "Jean", "jean.admin", "SecurePassword123!",
                true, false, UserRole.CURE, null
        );
        UserResponse expected = new UserResponse(
                UUID.randomUUID(), "Dupont", "Jean", "jean.admin",
                UserRole.ADMIN.name(), true, false
        );
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(parishId))
                .thenReturn(Optional.of(paroisse));
        when(userService.createUser(any(UserRequest.class), eq(requesterId))).thenReturn(expected);

        UserResponse result = service.assignAdminToParoisse(parishId, input, requesterId);

        ArgumentCaptor<UserRequest> captor = ArgumentCaptor.forClass(UserRequest.class);
        verify(userService).createUser(captor.capture(), eq(requesterId));
        UserRequest generated = captor.getValue();
        assertEquals(expected, result);
        assertEquals(UserRole.ADMIN, generated.role());
        assertFalse(generated.isGlobal());
        assertTrue(generated.isActive());
        assertEquals(1, generated.paroisses().size());
        assertEquals(parishId, generated.paroisses().get(0).getParoisseId());
        assertEquals("ADMIN", generated.paroisses().get(0).getRoleParoisse());
    }

    @Test
    void refusesAssignmentToMissingParish() {
        UUID parishId = UUID.randomUUID();
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(parishId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignAdminToParoisse(
                parishId,
                new UserRequest("Nom", "Prénom", "admin", "SecurePassword123!",
                        false, true, UserRole.ADMIN, List.<ParoisseAssignmentRequest>of()),
                UUID.randomUUID()
        ));

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void countsOnlyActiveNonDeletedParishes() {
        when(paroisseRepository.countByStatusDelFalseAndIsActiveTrue()).thenReturn(3L);

        assertEquals(3L, service.getActiveParoisseCount());
    }

    private Paroisse activeParish(UUID publicId) {
        Paroisse paroisse = new Paroisse();
        paroisse.setPublicId(publicId);
        paroisse.setIsActive(true);
        paroisse.setStatusDel(false);
        return paroisse;
    }
}
