package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.context.TenantContext;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre d'authentification JWT qui intercepte les requêtes HTTP entrantes.
 * Ce filtre s'exécute une fois par requête pour vérifier la présence et la validité
 * d'un token JWT dans l'en-tête d'autorisation.
 * Note: Cette classe n'est pas un @Component pour éviter les dépendances circulaires avec SecurityConfiguration.
 */
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtUtils.validateToken(token)) {

                String username = jwtUtils.getUsernameFromToken(token);
                UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

                /*
                 * Les informations d'accès sont relues depuis la base à chaque requête.
                 * Les claims tenant/isGlobal du JWT peuvent être anciens après une
                 * révocation d'accès, une réaffectation de paroisse ou un changement
                 * de statut utilisateur ; ils ne doivent donc pas piloter l'isolation.
                 */
                if (!userDetails.isEnabled()) {
                    throw new IllegalStateException("Compte utilisateur désactivé");
                }

                Long tenantId = userDetails.getTenantId();
                if (tenantId != null && !userDetails.isGlobal()) {
                    TenantContext.setCurrentTenant(tenantId);
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            log.debug("Authentification JWT impossible : {}", e.getMessage());
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
