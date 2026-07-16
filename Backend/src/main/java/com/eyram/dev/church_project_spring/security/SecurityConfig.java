package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.security.jwt.AuthEntryPointJwt;
import com.eyram.dev.church_project_spring.security.jwt.AuthTokenFilter;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPointJwt authEntryPointJwt;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        /*
         * Permet de distinguer une mauvaise configuration de paroisse
         * d'un simple mot de passe incorrect.
         */
        provider.setHideUserNotFoundExceptions(false);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthTokenFilter authTokenFilter(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            DaoAuthenticationProvider authenticationProvider,
            AuthTokenFilter authTokenFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Authentification et endpoints techniques publics
                        .requestMatchers("/auth/login", "/auth/login-multi-tenant").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Dépôt public et consultation publique par code
                        .requestMatchers(HttpMethod.POST, "/demandes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/demandes/code/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/facture/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/details-paiement/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/paroisses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-demandes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forfait-tarifs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horaires/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-paiement/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/demande-dates/demande/**").permitAll()

                        // Gestion des utilisateurs : aucun rôle métier inférieur ne doit accéder aux routes admin.
                        .requestMatchers("/admin/users", "/admin/users/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Le référentiel brut des affectations permet de modifier les tenants.
                        .requestMatchers("/paroisse-access", "/paroisse-access/**")
                        .access(globalSuperAdminAccess())

                        // Gestion du référentiel global
                        .requestMatchers(HttpMethod.POST, "/paroisses", "/localites", "/type-paiement")
                        .access(globalSuperAdminAccess())
                        .requestMatchers(HttpMethod.PUT, "/paroisses/**", "/localites/**", "/type-paiement/**")
                        .access(globalSuperAdminAccess())
                        .requestMatchers(HttpMethod.DELETE, "/paroisses/**", "/localites/**", "/type-paiement/**")
                        .access(globalSuperAdminAccess())

                        // Paramétrage propre à une paroisse
                        .requestMatchers(HttpMethod.POST, "/horaires", "/type-demandes", "/forfait-tarifs")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/horaires/**", "/type-demandes/**", "/forfait-tarifs/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/horaires/**", "/type-demandes/**", "/forfait-tarifs/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Gestion des demandes après leur dépôt public
                        .requestMatchers(HttpMethod.PUT, "/demandes/**")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/demandes/*/validation")
                        .hasAnyRole("CURE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/demandes/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Paiements : saisie par le secrétariat, suppression par un administrateur.
                        .requestMatchers(HttpMethod.POST, "/details-paiement")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/details-paiement/**")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/details-paiement/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Les factures sont générées automatiquement ; leur maintenance est administrative.
                        .requestMatchers(HttpMethod.POST, "/facture")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/facture/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/facture/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Les dates sont créées avec la demande ; seules les corrections administratives sont permises.
                        .requestMatchers(HttpMethod.POST, "/demande-dates")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/demande-dates/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/demande-dates/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> globalSuperAdminAccess() {
        return (authenticationSupplier, context) -> {
            var authentication = authenticationSupplier.get();
            boolean hasSuperAdminRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
            boolean isGlobal = authentication.getPrincipal() instanceof UserDetailsImpl principal
                    && principal.isGlobal();

            return new AuthorizationDecision(
                    authentication.isAuthenticated() && hasSuperAdminRole && isGlobal
            );
        };
    }
}
