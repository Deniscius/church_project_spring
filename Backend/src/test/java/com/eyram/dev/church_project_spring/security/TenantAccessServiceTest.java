package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAccessService tests")
class TenantAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;

    @Mock
    private Authentication authentication;

    private TenantAccessService tenantAccessService;

    @BeforeEach
    void setUp() {
        tenantAccessService = new TenantAccessService(userRepository, paroisseAccessRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Missing authentication is rejected")
    void missingAuthenticationIsRejected() {
        assertThrows(AccessDeniedException.class, tenantAccessService::getCurrentUser);
        verifyNoInteractions(userRepository, paroisseAccessRepository);
    }

    @Test
    @DisplayName("Inactive authenticated account is rejected")
    void inactiveAuthenticatedAccountIsRejected() {
        User user = user(false, false, UserRole.SECRETAIRE);
        authenticateAs(user.getPublicId());
        when(userRepository.findByPublicIdAndStatusDelFalse(user.getPublicId()))
                .thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, tenantAccessService::getCurrentUser);
    }

    @Test
    @DisplayName("Authenticated account without a global role is rejected")
    void accountWithoutRoleIsRejected() {
        User user = user(false, true, null);
        authenticateAs(user.getPublicId());
        when(userRepository.findByPublicIdAndStatusDelFalse(user.getPublicId()))
                .thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, tenantAccessService::getCurrentUser);
    }

    @Test
    @DisplayName("Global user accesses an existing parish but not a deleted parish")
    void globalUserAccessesOnlyExistingParish() {
        User user = user(true, true, UserRole.SUPER_ADMIN);
        Paroisse existingParish = paroisse(false);
        Paroisse deletedParish = paroisse(true);
        authenticateAndReload(user);

        assertTrue(tenantAccessService.canAccessParoisse(existingParish));
        assertFalse(tenantAccessService.canAccessParoisse(deletedParish));
        verifyNoInteractions(paroisseAccessRepository);
    }

    @Test
    @DisplayName("Local user access requires an active non-deleted assignment")
    void localUserAccessRequiresActiveAssignment() {
        User user = user(false, true, UserRole.SECRETAIRE);
        Paroisse paroisse = paroisse(false);
        authenticateAndReload(user);
        when(paroisseAccessRepository
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, paroisse))
                .thenReturn(true);

        assertTrue(tenantAccessService.canAccessParoisse(paroisse));
        verify(paroisseAccessRepository)
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, paroisse);
    }

    @Test
    @DisplayName("Local parish role requires an active assignment with a matching role")
    void localRoleRequiresActiveMatchingAssignment() {
        User user = user(false, true, UserRole.ADMIN);
        Paroisse paroisse = paroisse(false);
        ParoisseAccess access = new ParoisseAccess();
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(RoleParoisse.ADMIN);
        access.setActive(true);
        access.setStatusDel(false);
        authenticateAndReload(user);
        when(paroisseAccessRepository.findByUserAndParoisseAndStatusDelFalse(user, paroisse))
                .thenReturn(Optional.of(access));

        assertTrue(tenantAccessService.hasParoisseRole(
                paroisse,
                RoleParoisse.SECRETAIRE,
                RoleParoisse.ADMIN
        ));
        assertFalse(tenantAccessService.hasParoisseRole(paroisse, RoleParoisse.GESTIONNAIRE));
    }

    @Test
    @DisplayName("Role checks reject missing parish or missing accepted roles")
    void roleChecksRejectMissingInputs() {
        Paroisse paroisse = paroisse(false);

        assertFalse(tenantAccessService.hasParoisseRole(null, RoleParoisse.ADMIN));
        assertFalse(tenantAccessService.hasParoisseRole(paroisse));
        assertFalse(tenantAccessService.hasParoisseRole(paroisse, (RoleParoisse[]) null));
        verifyNoInteractions(userRepository, paroisseAccessRepository);
    }

    private void authenticateAndReload(User user) {
        authenticateAs(user.getPublicId());
        when(userRepository.findByPublicIdAndStatusDelFalse(user.getPublicId()))
                .thenReturn(Optional.of(user));
        assertSame(user, tenantAccessService.getCurrentUser());
    }

    private void authenticateAs(UUID publicId) {
        UserDetailsImpl principal = new UserDetailsImpl(
                publicId,
                "Utilisateur Test",
                "test.user",
                10L,
                false,
                "encoded-password",
                List.of(),
                true
        );

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private User user(boolean global, boolean active, UserRole role) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setNom("Koffi");
        user.setPrenom("Test");
        user.setUsername("test.user");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsGlobal(global);
        user.setIsActive(active);
        user.setStatusDel(false);
        return user;
    }

    private Paroisse paroisse(boolean deleted) {
        Paroisse paroisse = new Paroisse();
        paroisse.setId(10L);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Paroisse Test");
        paroisse.setAdresse("Lomé");
        paroisse.setIsActive(true);
        paroisse.setStatusDel(deleted);
        return paroisse;
    }
}
