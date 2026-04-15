package com.eyram.dev.church_project_spring.security.jwt;

import java.io.IOException;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eyram.dev.church_project_spring.security.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtre pour extraire et définir le contexte tenant (paroisse) depuis les headers ou JWT.
 * À ajouter dans la chaîne de filtres Spring Security.
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1️⃣ Essayer d'extraire le tenant du header
            String tenantHeader = request.getHeader("X-Tenant-ID");
            if (tenantHeader != null && !tenantHeader.isEmpty()) {
                try {
                    UUID tenantId = UUID.fromString(tenantHeader);
                    TenantContext.setTenantId(tenantId);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid tenant ID format in header: " + tenantHeader);
                }
            }

            // 2️⃣ Continuer la requête
            filterChain.doFilter(request, response);
        } finally {
            // 3️⃣ Nettoyer le contexte ThreadLocal
            TenantContext.clear();
        }
    }
}
