package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Défense CSRF pour auth cookie : les mutations authentifiées par cookie
 * exigent un header custom (préflight CORS), ce qui bloque les posts cross-site
 * classiques. Complète SameSite=Lax.
 */
@Component
public class CookieAuthMutationGuardFilter extends OncePerRequestFilter {

    private static final Set<String> UNSAFE = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final JwtProperties jwtProperties;

    public CookieAuthMutationGuardFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase(Locale.ROOT);
        if (!UNSAFE.contains(method) || !hasAuthCookie(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String xhr = request.getHeader("X-Requested-With");
        if (StringUtils.hasText(xhr) && "XMLHttpRequest".equalsIgnoreCase(xhr.trim())) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Requête refusée (en-tête X-Requested-With requis).\"}");
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        String name = jwtProperties.cookieName();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }
}
