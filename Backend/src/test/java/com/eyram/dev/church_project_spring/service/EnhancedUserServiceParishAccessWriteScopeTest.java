package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.request.ParoisseAssignmentRequest;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService parish access write scope tests")
class EnhancedUserServiceParishAccessWriteScopeTest {

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
    private UUID targetId;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Local administrator cannot assign a different parish")
    void localAdministratorCannotAssignDifferentParish() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse ownParoisse = paroisse(10L);
        Paroisse otherParoisse = paroisse(20L);

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, ownParoisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, ownParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.assignParoisseToUser(
                        targetId,
                        assignment(otherParoisse),
                        requesterId
                )
        );

        verify(paroisseRepository, never()).findByPublicIdAndStatusDelFalse(any(UUID.class));
        verify(paroisseAccessRepository, never()).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Local administrator cannot assign a parish to a user from another parish")
    void localAdministratorCannotAssignToAnotherParishUser() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "other.user");
        Paroisse requesterParoisse = paroisse(10L);
        Paroisse targetParoisse = paroisse(20L);

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, targetParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.assignParoisseToUser(
                        targetId,
                        assignment(requesterParoisse),
                        requesterId
                )
        );

        verify(paroisseAccessRepository, never()).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Local administrator cannot revoke access to another parish by public ID")
    void localAdministratorCannotRevokeAnotherParish() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse ownParoisse = paroisse(10L);
        UUID otherParoissePublicId = UUID.randomUUID();

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, ownParoisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, ownParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.revokeParoisseAccess(
                        targetId,
                        otherParoissePublicId,
                        requesterId
                )
        );

        verify(paroisseRepository, never())
                .findByPublicIdAndStatusDelFalse(otherParoissePublicId);
        verify(paroisseAccessRepository, never()).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Global administrator can assign any active parish")
    void globalAdministratorCanAssignAnyParish() {
        User requester = user(requesterId, true, UserRole.SUPER_ADMIN, "global.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse paroisse = paroisse(20L);
        ParoisseAssignmentRequest assignment = assignment(paroisse);

        mockUserLookup(requester, target);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.existsByUserAndParoisseAndStatusDelFalse(target, paroisse))
                .thenReturn(false);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of());

        userService.assignParoisseToUser(targetId, assignment, requesterId);

        verify(paroisseAccessRepository).save(any(ParoisseAccess.class));
    }

    @Test
    @DisplayName("Global administrator can revoke access using a public parish ID")
    void globalAdministratorCanRevokeAnyParish() {
        User requester = user(requesterId, true, UserRole.SUPER_ADMIN, "global.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse paroisse = paroisse(20L);
        ParoisseAccess access = activeAccess(target, paroisse);

        mockUserLookup(requester, target);
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.findByUserAndParoisseAndStatusDelFalse(target, paroisse))
                .thenReturn(Optional.of(access));

        userService.revokeParoisseAccess(targetId, paroisse.getPublicId(), requesterId);

        verify(paroisseAccessRepository).save(access);
    }

    private void mockUserLookup(User requester, User target) {
        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(userRepository.findByPublicIdAndStatusDelFalse(targetId))
                .thenReturn(Optional.of(target));
    }

    private ParoisseAssignmentRequest assignment(Paroisse paroisse) {
        return ParoisseAssignmentRequest.builder()
                .paroisseId(paroisse.getPublicId())
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
