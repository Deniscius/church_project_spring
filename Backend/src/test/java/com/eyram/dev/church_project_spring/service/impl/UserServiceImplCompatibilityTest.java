package com.eyram.dev.church_project_spring.service.impl;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import com.eyram.dev.church_project_spring.service.EnhancedUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Legacy user service compatibility facade")
class UserServiceImplCompatibilityTest {

    @Mock
    private EnhancedUserService enhancedUserService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID actorId;
    private UUID userId;
    private UserRequest request;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        request = new UserRequest(
                "Dupont",
                "Jean",
                "jean.dupont",
                "SecurePassword123!",
                true,
                true,
                UserRole.ADMIN,
                null
        );
        response = new UserResponse(
                userId,
                "Dupont",
                "Jean",
                "jean.dupont",
                "ADMIN",
                true,
                true
        );

        UserDetailsImpl principal = new UserDetailsImpl(
                actorId,
                "Administrateur Test",
                "admin.test",
                null,
                true,
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                true
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create delegates to the canonical service with the authenticated actor")
    void createDelegatesToCanonicalService() {
        when(enhancedUserService.createUser(request, actorId)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertSame(response, result);
        verify(enhancedUserService).createUser(request, actorId);
    }

    @Test
    @DisplayName("Update delegates to the canonical service with the authenticated actor")
    void updateDelegatesToCanonicalService() {
        when(enhancedUserService.updateUser(userId, request, actorId)).thenReturn(response);

        UserResponse result = userService.update(userId, request);

        assertSame(response, result);
        verify(enhancedUserService).updateUser(userId, request, actorId);
    }

    @Test
    @DisplayName("Read delegates to the canonical service with the authenticated actor")
    void getByPublicIdDelegatesToCanonicalService() {
        when(enhancedUserService.getUserByPublicId(userId, actorId)).thenReturn(response);

        UserResponse result = userService.getByPublicId(userId);

        assertSame(response, result);
        verify(enhancedUserService).getUserByPublicId(userId, actorId);
    }

    @Test
    @DisplayName("List delegates to the canonical service with the authenticated actor")
    void getAllDelegatesToCanonicalService() {
        when(enhancedUserService.getAllActiveUsers(actorId)).thenReturn(List.of(response));

        List<UserResponse> result = userService.getAll();

        assertEquals(List.of(response), result);
        verify(enhancedUserService).getAllActiveUsers(actorId);
    }

    @Test
    @DisplayName("Delete delegates to the canonical service with the authenticated actor")
    void deleteDelegatesToCanonicalService() {
        userService.delete(userId);

        verify(enhancedUserService).deleteUser(userId, actorId);
    }
}
