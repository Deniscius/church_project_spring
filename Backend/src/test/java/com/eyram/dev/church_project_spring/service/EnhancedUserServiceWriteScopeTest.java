package com.eyram.dev.church_project_spring.service;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService tenant-scoped write tests")
class EnhancedUserServiceWriteScopeTest {

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
    @DisplayName("Global administrator can update a user from any parish")
    void globalAdministratorCanUpdateAnyUser() {
        User requester = user(requesterId, true, UserRole.SUPER_ADMIN, "global.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        UserRequest request = updateRequest(false, UserRole.SECRETAIRE);

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(userRepository.findByPublicIdAndStatusDelFalse(targetId))
                .thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var response = userService.updateUser(targetId, request, requesterId);

        assertEquals(targetId, response.publicId());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Local administrator can update a user from the same parish")
    void localAdministratorCanUpdateSameParishUser() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse paroisse = paroisse(10L);
        UserRequest request = updateRequest(false, UserRole.SECRETAIRE);

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, paroisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, paroisse)));
        when(userRepository.save(target)).thenReturn(target);

        var response = userService.updateUser(targetId, request, requesterId);

        assertEquals(targetId, response.publicId());
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Local administrator cannot update a user from another parish")
    void localAdministratorCannotUpdateAnotherParishUser() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "other.user");

        mockDifferentParishes(requester, target);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.updateUser(
                        targetId,
                        updateRequest(false, UserRole.SECRETAIRE),
                        requesterId
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Local administrator cannot grant global privileges")
    void localAdministratorCannotGrantGlobalPrivileges() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "local.user");
        Paroisse paroisse = paroisse(10L);

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, paroisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, paroisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.updateUser(
                        targetId,
                        updateRequest(true, UserRole.SUPER_ADMIN),
                        requesterId
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Local administrator cannot delete a user from another parish")
    void localAdministratorCannotDeleteAnotherParishUser() {
        User requester = user(requesterId, false, UserRole.ADMIN, "local.admin");
        User target = user(targetId, false, UserRole.SECRETAIRE, "other.user");

        mockDifferentParishes(requester, target);

        assertThrows(
                AccessDeniedException.class,
                () -> userService.deleteUser(targetId, requesterId)
        );

        assertFalse(target.getStatusDel());
        verify(userRepository, never()).save(any(User.class));
    }

    private void mockUserLookup(User requester, User target) {
        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(userRepository.findByPublicIdAndStatusDelFalse(targetId))
                .thenReturn(Optional.of(target));
    }

    private void mockDifferentParishes(User requester, User target) {
        Paroisse requesterParoisse = paroisse(10L);
        Paroisse targetParoisse = paroisse(20L);

        mockUserLookup(requester, target);
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, targetParoisse)));
    }

    private UserRequest updateRequest(boolean global, UserRole role) {
        return new UserRequest(
                "Koffi",
                "Test",
                "local.user",
                "",
                global,
                true,
                role,
                null
        );
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
