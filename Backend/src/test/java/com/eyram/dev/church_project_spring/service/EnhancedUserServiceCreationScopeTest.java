package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService tenant-scoped creation tests")
class EnhancedUserServiceCreationScopeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParoisseRepository paroisseRepository;

    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EnhancedUserService userService;

    private UUID requesterId;
    private Paroisse requesterParoisse;
    private User requester;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        requesterParoisse = paroisse(10L);
        requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
    }

    @Test
    @DisplayName("Local administrator can create a user in the assigned parish")
    void localAdministratorCanCreateUserInOwnParish() {
        UUID createdUserId = UUID.randomUUID();
        User savedUser = user(createdUserId, false, UserRole.SECRETAIRE, "new.user");
        UserRequest request = localRequest(
                UserRole.SECRETAIRE,
                assignment(requesterParoisse.getPublicId())
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));
        when(userRepository.existsByUsernameAndStatusDelFalse("new.user")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(requesterParoisse.getPublicId()))
                .thenReturn(Optional.of(requesterParoisse));
        when(paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(savedUser, requesterParoisse))
                .thenReturn(false);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(savedUser))
                .thenReturn(List.of());

        var response = userService.createUser(request, requesterId);

        assertEquals(createdUserId, response.publicId());
        verify(userRepository).save(any(User.class));
        verify(paroisseAccessRepository).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Local administrator cannot create a global user")
    void localAdministratorCannotCreateGlobalUser() {
        UserRequest request = new UserRequest(
                "Koffi",
                "Test",
                "global.user",
                "SecurePassword123!",
                true,
                true,
                UserRole.ADMIN,
                null
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.createUser(request, requesterId)
        );

        verify(userRepository, never()).existsByUsernameAndStatusDelFalse(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Local administrator cannot create a super administrator")
    void localAdministratorCannotCreateSuperAdministrator() {
        UserRequest request = localRequest(
                UserRole.SUPER_ADMIN,
                assignment(requesterParoisse.getPublicId())
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.createUser(request, requesterId)
        );

        verify(userRepository, never()).existsByUsernameAndStatusDelFalse(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Local administrator cannot create a user in another parish")
    void localAdministratorCannotCreateUserInAnotherParish() {
        Paroisse otherParoisse = paroisse(20L);
        UserRequest request = localRequest(
                UserRole.SECRETAIRE,
                assignment(otherParoisse.getPublicId())
        );

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.createUser(request, requesterId)
        );

        verify(paroisseRepository, never()).findByPublicIdAndStatusDelFalse(any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }

    private UserRequest localRequest(
            UserRole role,
            ParoisseAssignmentRequest assignment
    ) {
        return new UserRequest(
                "Koffi",
                "Test",
                "new.user",
                "SecurePassword123!",
                false,
                true,
                role,
                List.of(assignment)
        );
    }

    private ParoisseAssignmentRequest assignment(UUID paroissePublicId) {
        return ParoisseAssignmentRequest.builder()
                .paroisseId(paroissePublicId)
                .roleParoisse("SECRETAIRE")
                .build();
    }

    private User user(UUID publicId, boolean global, UserRole role, String username) {
        User user = new User();
        user.setPublicId(publicId);
        user.setNom("Koffi");
        user.setPrenom("Test");
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsGlobal(global);
        user.setIsActive(true);
        user.setStatusDel(false);
        return user;
    }

    private Paroisse paroisse(Long id) {
        Paroisse paroisse = new Paroisse();
        paroisse.setId(id);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Paroisse " + id);
        paroisse.setStatusDel(false);
        paroisse.setIsActive(true);
        return paroisse;
    }

    private ParoisseAccess activeAccess(User user, Paroisse paroisse) {
        ParoisseAccess access = new ParoisseAccess();
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setActive(true);
        access.setStatusDel(false);
        return access;
    }
}
