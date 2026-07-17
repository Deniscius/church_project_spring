package com.eyram.dev.church_project_spring.service;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedUserService tenant-scoped read tests")
class EnhancedUserServiceReadScopeTest {

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
    @DisplayName("Global administrator can list all active users")
    void globalAdministratorCanListAllUsers() {
        User requester = user(requesterId, true, "global.admin");
        User target = user(targetId, false, "local.user");

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(userRepository.findByStatusDelFalseOrderByNomAscPrenomAsc())
                .thenReturn(List.of(requester, target));

        var result = userService.getAllActiveUsers(requesterId);

        assertEquals(2, result.size());
        verify(userRepository).findByStatusDelFalseOrderByNomAscPrenomAsc();
    }

    @Test
    @DisplayName("Global administrator can list parish users using the public parish ID")
    void globalAdministratorCanListParishByPublicId() {
        User requester = user(requesterId, true, "global.admin");
        User target = user(targetId, false, "local.user");
        Paroisse paroisse = paroisse(10L);
        ParoisseAccess targetAccess = activeAccess(target, paroisse);

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(paroisseRepository.findByPublicIdAndStatusDelFalse(paroisse.getPublicId()))
                .thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse))
                .thenReturn(List.of(targetAccess));

        var result = userService.getUsersByParoisse(paroisse.getPublicId(), requesterId);

        assertEquals(1, result.size());
        assertEquals(targetId, result.get(0).publicId());
    }

    @Test
    @DisplayName("Local administrator only lists users from the assigned parish")
    void localAdministratorListsOnlyOwnParishUsers() {
        User requester = user(requesterId, false, "local.admin");
        User target = user(targetId, false, "local.user");
        Paroisse paroisse = paroisse(10L);
        ParoisseAccess requesterAccess = activeAccess(requester, paroisse);
        ParoisseAccess targetAccess = activeAccess(target, paroisse);

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(requesterAccess));
        when(paroisseRepository.findById(10L)).thenReturn(Optional.of(paroisse));
        when(paroisseAccessRepository.findByParoisseAndStatusDelFalse(paroisse))
                .thenReturn(List.of(targetAccess));

        var result = userService.getAllActiveUsers(requesterId);

        assertEquals(1, result.size());
        assertEquals(targetId, result.get(0).publicId());
        verify(userRepository, never()).findByStatusDelFalseOrderByNomAscPrenomAsc();
    }

    @Test
    @DisplayName("Local administrator cannot read a user from another parish")
    void localAdministratorCannotReadUserFromAnotherParish() {
        User requester = user(requesterId, false, "local.admin");
        User target = user(targetId, false, "other.user");
        Paroisse requesterParoisse = paroisse(10L);
        Paroisse targetParoisse = paroisse(20L);

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(userRepository.findByPublicIdAndStatusDelFalse(targetId))
                .thenReturn(Optional.of(target));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(target))
                .thenReturn(List.of(activeAccess(target, targetParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.getUserByPublicId(targetId, requesterId)
        );
    }

    @Test
    @DisplayName("Local administrator cannot list another parish by public ID")
    void localAdministratorCannotListAnotherParish() {
        User requester = user(requesterId, false, "local.admin");
        Paroisse requesterParoisse = paroisse(10L);
        UUID otherParoissePublicId = UUID.randomUUID();

        when(userRepository.findByPublicIdAndStatusDelFalse(requesterId))
                .thenReturn(Optional.of(requester));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(requester))
                .thenReturn(List.of(activeAccess(requester, requesterParoisse)));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.getUsersByParoisse(otherParoissePublicId, requesterId)
        );

        verify(paroisseRepository, never())
                .findByPublicIdAndStatusDelFalse(otherParoissePublicId);
    }

    private User user(UUID publicId, boolean global, String username) {
        User user = new User();
        user.setPublicId(publicId);
        user.setNom("Koffi");
        user.setPrenom("Test");
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(global ? UserRole.SUPER_ADMIN : UserRole.ADMIN);
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
