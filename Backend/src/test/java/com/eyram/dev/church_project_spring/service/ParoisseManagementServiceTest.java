package com.eyram.dev.church_project_spring.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.entities.Localite;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.utils.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParoisseManagementService Tests")
class ParoisseManagementServiceTest {

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private EnhancedUserService enhancedUserService;

    @InjectMocks
    private ParoisseManagementService paroisseManagementService;

    private UUID testCreatedBy;
    private Paroisse testParoisse;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        testCreatedBy = UUID.randomUUID();

        Localite testLocalite = new Localite();
        testLocalite.setId(1L);
        testLocalite.setPublicId(UUID.randomUUID());
        testLocalite.setVille("Lomé");
        testLocalite.setQuartier("Tokoin");
        testLocalite.setStatusDel(false);

        testParoisse = new Paroisse();
        testParoisse.setId(1L);
        testParoisse.setPublicId(UUID.randomUUID());
        testParoisse.setNom("Paroisse Saint-Martin");
        testParoisse.setAdresse("123 Rue de l'Église");
        testParoisse.setEmail("contact@saint-martin.fr");
        testParoisse.setTelephone("+33123456789");
        testParoisse.setIsActive(true);
        testParoisse.setStatusDel(false);
        testParoisse.setLocalite(testLocalite);

        testAdmin = new User();
        testAdmin.setId(1L);
        testAdmin.setPublicId(UUID.randomUUID());
        testAdmin.setNom("Dupont");
        testAdmin.setPrenom("Jean");
        testAdmin.setUsername("jean.dupont.admin");
        testAdmin.setRole(UserRole.ADMIN);
        testAdmin.setIsGlobal(false);
        testAdmin.setIsActive(true);
        testAdmin.setStatusDel(false);
    }

    @Test
    @DisplayName("Should create paroisse successfully")
    void testCreateParoisseSuccess() {
        when(paroisseRepository.findAllByStatusDelFalse()).thenReturn(List.of());
        when(paroisseRepository.save(any(Paroisse.class))).thenReturn(testParoisse);

        Paroisse created = paroisseManagementService.createParoisse(testParoisse, testCreatedBy);

        assertNotNull(created);
        assertEquals("Paroisse Saint-Martin", created.getNom());
        assertNotNull(created.getLocalite());
        verify(paroisseRepository).findAllByStatusDelFalse();
        verify(paroisseRepository).save(any(Paroisse.class));
    }

    @Test
    @DisplayName("Should update paroisse successfully")
    void testUpdateParoisseSuccess() {
        when(paroisseRepository.findById(1L)).thenReturn(Optional.of(testParoisse));
        when(paroisseRepository.save(any(Paroisse.class))).thenReturn(testParoisse);

        Paroisse updateData = new Paroisse();
        updateData.setNom("Paroisse Saint-Martin Updated");

        Paroisse updated = paroisseManagementService.updateParoisse(1L, updateData, testCreatedBy);

        assertNotNull(updated);
        verify(paroisseRepository, times(1)).save(any(Paroisse.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when paroisse not found for update")
    void testUpdateParoisseNotFound() {
        when(paroisseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                paroisseManagementService.updateParoisse(999L, testParoisse, testCreatedBy));
    }

    @Test
    @DisplayName("Should deactivate paroisse successfully")
    void testDeactivateParoisseSuccess() {
        when(paroisseRepository.findById(1L)).thenReturn(Optional.of(testParoisse));
        when(paroisseRepository.save(any(Paroisse.class))).thenReturn(testParoisse);

        paroisseManagementService.deactivateParoisse(1L, testCreatedBy);

        verify(paroisseRepository, times(1)).save(any(Paroisse.class));
        assertTrue(testParoisse.getStatusDel());
    }

    @Test
    @DisplayName("Should create a local admin with its parish in one canonical service call")
    void testAssignAdminToParoisseAtomically() {
        UserRequest input = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont.admin",
                "SecurePassword123!",
                true,
                false,
                UserRole.CURE,
                null
        );
        UserResponse expected = new UserResponse(
                testAdmin.getPublicId(),
                testAdmin.getNom(),
                testAdmin.getPrenom(),
                testAdmin.getUsername(),
                UserRole.ADMIN.name(),
                testAdmin.getIsActive(),
                testAdmin.getIsGlobal()
        );

        when(paroisseRepository.findById(1L)).thenReturn(Optional.of(testParoisse));
        when(enhancedUserService.createUser(any(UserRequest.class), eq(testCreatedBy)))
                .thenReturn(expected);

        UserResponse result = paroisseManagementService.assignAdminToParoisse(
                1L,
                input,
                testCreatedBy
        );

        ArgumentCaptor<UserRequest> requestCaptor = ArgumentCaptor.forClass(UserRequest.class);
        verify(enhancedUserService).createUser(requestCaptor.capture(), eq(testCreatedBy));
        verify(enhancedUserService, never()).assignParoisseToUser(
                any(UUID.class),
                any(ParoisseAssignmentRequest.class),
                any(UUID.class)
        );

        UserRequest adminRequest = requestCaptor.getValue();
        assertSame(expected, result);
        assertFalse(adminRequest.isGlobal());
        assertTrue(adminRequest.isActive());
        assertEquals(UserRole.ADMIN, adminRequest.role());
        assertNotNull(adminRequest.paroisses());
        assertEquals(1, adminRequest.paroisses().size());
        assertEquals(testParoisse.getPublicId(), adminRequest.paroisses().get(0).getParoisseId());
        assertEquals("ADMIN", adminRequest.paroisses().get(0).getRoleParoisse());
    }

    @Test
    @DisplayName("Should not create an admin when the parish does not exist")
    void testAssignAdminToMissingParoisse() {
        UserRequest input = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont.admin",
                "SecurePassword123!",
                false,
                true,
                UserRole.ADMIN,
                null
        );
        when(paroisseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> paroisseManagementService.assignAdminToParoisse(999L, input, testCreatedBy)
        );

        verifyNoInteractions(enhancedUserService);
    }

    @Test
    @DisplayName("Should get paroisse by ID successfully")
    void testGetParoisseByIdSuccess() {
        when(paroisseRepository.findById(1L)).thenReturn(Optional.of(testParoisse));

        Paroisse found = paroisseManagementService.getParoisseById(1L);

        assertNotNull(found);
        assertEquals("Paroisse Saint-Martin", found.getNom());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when paroisse not found for get")
    void testGetParoisseByIdNotFound() {
        when(paroisseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                paroisseManagementService.getParoisseById(999L));
    }

    @Test
    @DisplayName("Should get all active paroisses")
    void testGetAllActiveParoisses() {
        when(paroisseRepository.findAllByStatusDelFalse()).thenReturn(List.of(testParoisse));

        var paroisses = paroisseManagementService.getAllActiveParoisses();

        assertNotNull(paroisses);
        assertEquals(1, paroisses.size());
        verify(paroisseRepository, times(1)).findAllByStatusDelFalse();
    }

    @Test
    @DisplayName("Should get active paroisse count")
    void testGetActiveParoisseCount() {
        when(paroisseRepository.count()).thenReturn(1L);

        long count = paroisseManagementService.getActiveParoisseCount();

        assertEquals(1L, count);
        verify(paroisseRepository, times(1)).count();
    }
}
