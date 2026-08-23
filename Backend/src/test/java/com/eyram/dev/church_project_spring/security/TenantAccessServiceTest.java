package com.eyram.dev.church_project_spring.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TenantAccessServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;
    @Mock
    private TenantCatalogBootstrapService tenantCatalogBootstrapService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void localUserCanAccessAssignedParishButNotAnotherOne() {
        User user = user(false);
        Paroisse assigned = parish(1L);
        Paroisse other = parish(2L);
        authenticate(user, assigned.getId());
        when(userRepository.findByPublicIdAndStatusDelFalse(user.getPublicId()))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, assigned))
                .thenReturn(true);
        when(paroisseAccessRepository
                .existsByUserAndParoisseAndActiveTrueAndStatusDelFalse(user, other))
                .thenReturn(false);

        TenantAccessService service = service();

        assertTrue(service.canAccessParoisse(assigned));
        assertFalse(service.canAccessParoisse(other));
        assertThrows(AccessDeniedException.class, () -> service.checkParoisseAccess(other));
    }

    @Test
    void globalPrincipalCanAccessAnyExistingParishWithoutLocalAssignment() {
        User user = user(true);
        Paroisse parish = parish(2L);
        authenticate(user, null);

        TenantAccessService service = service();

        assertTrue(service.canAccessParoisse(parish));
        verify(userRepository, never()).findByPublicIdAndStatusDelFalse(user.getPublicId());
    }

    @Test
    void deletedParishIsDeniedEvenToGlobalPrincipal() {
        User user = user(true);
        Paroisse parish = parish(2L);
        parish.setStatusDel(true);
        authenticate(user, null);

        assertFalse(service().canAccessParoisse(parish));
    }

    private TenantAccessService service() {
        return new TenantAccessService(
                userRepository,
                paroisseAccessRepository,
                tenantCatalogBootstrapService
        );
    }

    private static User user(boolean global) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setNom("Test");
        user.setPrenom("Tenant");
        user.setUsername(global ? "global.test" : "local.test");
        user.setPassword("encoded");
        user.setRole(UserRole.SUPER_ADMIN);
        user.setIsActive(true);
        user.setIsGlobal(global);
        user.setStatusDel(false);
        user.setTokenVersion(0L);
        return user;
    }

    private static Paroisse parish(Long id) {
        Paroisse parish = new Paroisse();
        parish.setId(id);
        parish.setPublicId(UUID.randomUUID());
        parish.setStatusDel(false);
        return parish;
    }

    private static void authenticate(User user, Long tenantId) {
        UserDetailsImpl principal = UserDetailsImpl.build(user, tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
