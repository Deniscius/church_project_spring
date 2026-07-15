package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.context.HibernateTenantFilterActivator;
import com.eyram.dev.church_project_spring.context.TenantContext;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenFilter tests")
class AuthTokenFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HibernateTenantFilterActivator tenantFilterActivator;

    private AuthTokenFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new AuthTokenFilter(jwtUtils, userDetailsService, tenantFilterActivator);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Current database tenant is used instead of stale JWT claims")
    void currentDatabaseTenantIsUsed() throws Exception {
        UserDetailsImpl currentUser = userDetails(42L, false, true);
        AtomicReference<Long> tenantSeenByChain = new AtomicReference<>();
        AtomicReference<Object> principalSeenByChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) -> {
            tenantSeenByChain.set(TenantContext.getCurrentTenant());
            principalSeenByChain.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        };

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("admin.local");
        when(userDetailsService.loadUserByUsername("admin.local")).thenReturn(currentUser);

        filter.doFilterInternal(request, response, chain);

        assertEquals(42L, tenantSeenByChain.get());
        assertSame(currentUser, principalSeenByChain.get());
        verify(tenantFilterActivator).activateFilter();
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    @DisplayName("Current global user is never restricted by a stale tenant claim")
    void currentGlobalUserIsNotTenantRestricted() throws Exception {
        UserDetailsImpl currentUser = userDetails(null, true, true);
        AtomicReference<Long> tenantSeenByChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                tenantSeenByChain.set(TenantContext.getCurrentTenant());

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("super.admin");
        when(userDetailsService.loadUserByUsername("super.admin")).thenReturn(currentUser);

        filter.doFilterInternal(request, response, chain);

        assertNull(tenantSeenByChain.get());
        verify(tenantFilterActivator, never()).activateFilter();
    }

    @Test
    @DisplayName("Disabled users are not authenticated with an old token")
    void disabledUserIsNotAuthenticated() throws Exception {
        UserDetailsImpl disabledUser = userDetails(42L, false, false);
        AtomicReference<Object> authenticationSeenByChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                authenticationSeenByChain.set(SecurityContextHolder.getContext().getAuthentication());

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("disabled.user");
        when(userDetailsService.loadUserByUsername("disabled.user")).thenReturn(disabledUser);

        filter.doFilterInternal(request, response, chain);

        assertNull(authenticationSeenByChain.get());
        verify(tenantFilterActivator, never()).activateFilter();
    }

    private UserDetailsImpl userDetails(Long tenantId, boolean global, boolean enabled) {
        return new UserDetailsImpl(
                UUID.randomUUID(),
                "Utilisateur Test",
                "test.user",
                tenantId,
                global,
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                enabled
        );
    }
}
