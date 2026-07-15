package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.RoleParoisse;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.dto.LoginRequest;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import com.eyram.dev.church_project_spring.utils.exception.AccountDisabledException;
import com.eyram.dev.church_project_spring.utils.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiTenantAuthService tests")
class MultiTenantAuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParoisseAccessRepository paroisseAccessRepository;

    @InjectMocks
    private MultiTenantAuthService authService;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("local.user", "SecurePassword123!");
    }

    @Test
    @DisplayName("Local user logs in with exactly one active parish")
    void localUserLogsInWithOneActiveParish() {
        User user = user(false, true, UserRole.SECRETAIRE, "local.user");
        Paroisse paroisse = paroisse(10L, true, false);
        ParoisseAccess access = access(user, paroisse, RoleParoisse.SECRETAIRE);
        UserDetailsImpl principal = authenticateAs(user, 10L);

        when(userRepository.findByUsernameAndStatusDelFalse("local.user"))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of(access));
        when(jwtUtils.generateToken(principal)).thenReturn("jwt-token");

        var response = authService.loginMultiTenant(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals(1, response.getParoisses().size());
        assertNotNull(response.getSelectedParoisse());
        assertEquals(paroisse.getPublicId(), response.getSelectedParoisse().getParoisseId());
    }

    @Test
    @DisplayName("Global user logs in without a selected parish")
    void globalUserLogsInWithoutSelectedParish() {
        User user = user(true, true, UserRole.SUPER_ADMIN, "local.user");
        UserDetailsImpl principal = authenticateAs(user, null);

        when(userRepository.findByUsernameAndStatusDelFalse("local.user"))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of());
        when(jwtUtils.generateToken(principal)).thenReturn("global-jwt");

        var response = authService.loginMultiTenant(loginRequest);

        assertEquals("global-jwt", response.getToken());
        assertEquals(0, response.getParoisses().size());
        assertNull(response.getSelectedParoisse());
    }

    @Test
    @DisplayName("Local user without an active parish is rejected")
    void localUserWithoutActiveParishIsRejected() {
        User user = user(false, true, UserRole.SECRETAIRE, "local.user");
        authenticateAs(user, 10L);

        when(userRepository.findByUsernameAndStatusDelFalse("local.user"))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of());

        assertThrows(
                AccountDisabledException.class,
                () -> authService.loginMultiTenant(loginRequest)
        );

        verify(jwtUtils, never()).generateToken(any(UserDetailsImpl.class));
    }

    @Test
    @DisplayName("Local user with several active parishes is rejected")
    void localUserWithSeveralActiveParishesIsRejected() {
        User user = user(false, true, UserRole.SECRETAIRE, "local.user");
        Paroisse first = paroisse(10L, true, false);
        Paroisse second = paroisse(20L, true, false);
        authenticateAs(user, 10L);

        when(userRepository.findByUsernameAndStatusDelFalse("local.user"))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of(
                        access(user, first, RoleParoisse.SECRETAIRE),
                        access(user, second, RoleParoisse.SECRETAIRE)
                ));

        assertThrows(
                AccountDisabledException.class,
                () -> authService.loginMultiTenant(loginRequest)
        );

        verify(jwtUtils, never()).generateToken(any(UserDetailsImpl.class));
    }

    @Test
    @DisplayName("Access to an inactive parish is ignored and login is rejected")
    void inactiveParishAccessIsRejected() {
        User user = user(false, true, UserRole.SECRETAIRE, "local.user");
        Paroisse inactiveParoisse = paroisse(10L, false, false);
        authenticateAs(user, 10L);

        when(userRepository.findByUsernameAndStatusDelFalse("local.user"))
                .thenReturn(Optional.of(user));
        when(paroisseAccessRepository.findByUserAndActiveTrueAndStatusDelFalse(user))
                .thenReturn(List.of(access(user, inactiveParoisse, RoleParoisse.SECRETAIRE)));

        assertThrows(
                AccountDisabledException.class,
                () -> authService.loginMultiTenant(loginRequest)
        );

        verify(jwtUtils, never()).generateToken(any(UserDetailsImpl.class));
    }

    @Test
    @DisplayName("Authentication provider failures remain server errors")
    void authenticationProviderFailureIsNotConvertedToInvalidCredentials() {
        AuthenticationServiceException providerFailure =
                new AuthenticationServiceException("provider unavailable");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(providerFailure);

        AuthenticationServiceException thrown = assertThrows(
                AuthenticationServiceException.class,
                () -> authService.loginMultiTenant(loginRequest)
        );

        assertEquals(providerFailure, thrown);
        verify(userRepository, never()).findByUsernameAndStatusDelFalse(any(String.class));
    }

    @Test
    @DisplayName("Bad credentials are mapped to a safe unauthorized error")
    void badCredentialsAreMappedToInvalidCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad password"));

        InvalidCredentialsException thrown = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.loginMultiTenant(loginRequest)
        );

        assertEquals("Identifiants incorrects", thrown.getMessage());
        verify(userRepository, never()).findByUsernameAndStatusDelFalse(any(String.class));
    }

    private UserDetailsImpl authenticateAs(User user, Long tenantId) {
        UserDetailsImpl principal = UserDetailsImpl.build(user, tenantId);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        return principal;
    }

    private User user(boolean global, boolean active, UserRole role, String username) {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setNom("Koffi");
        user.setPrenom("Test");
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsGlobal(global);
        user.setIsActive(active);
        user.setStatusDel(false);
        return user;
    }

    private Paroisse paroisse(Long id, boolean active, boolean deleted) {
        Paroisse paroisse = new Paroisse();
        paroisse.setId(id);
        paroisse.setPublicId(UUID.randomUUID());
        paroisse.setNom("Paroisse " + id);
        paroisse.setAdresse("Lomé");
        paroisse.setIsActive(active);
        paroisse.setStatusDel(deleted);
        return paroisse;
    }

    private ParoisseAccess access(
            User user,
            Paroisse paroisse,
            RoleParoisse role
    ) {
        ParoisseAccess access = new ParoisseAccess();
        access.setUser(user);
        access.setParoisse(paroisse);
        access.setRoleParoisse(role);
        access.setActive(true);
        access.setStatusDel(false);
        return access;
    }
}
