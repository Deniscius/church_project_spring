package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.security.jwt.AuthEntryPointJwt;
import com.eyram.dev.church_project_spring.security.jwt.AuthTokenFilter;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPointJwt authEntryPointJwt;
    private final Environment environment;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        /*
         * Masque les détails d'échec d'authentification pour limiter
         * l'énumération de comptes / mauvaises configs de paroisse.
         */
        provider.setHideUserNotFoundExceptions(true);

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
                // Empêche la popup navigateur « Se connecter » (WWW-Authenticate: Basic).
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.contentTypeOptions(contentType -> {});
                    headers.frameOptions(frame -> frame.sameOrigin());
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                    ));
                    headers.permissionsPolicy(permissions -> permissions.policy(
                            "camera=(), microphone=(), geolocation=()"
                    ));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000)
                    );
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Authentification et endpoints techniques publics
                        .requestMatchers("/auth/login", "/auth/login-multi-tenant").permitAll()
                        // Swagger : jamais en production (même si springdoc était réactivé par erreur).
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .access(swaggerAccess())
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error", "/login", "/login.html", "/health-ui", "/health.html", "/assets/**", "/").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Dépôt public et consultation publique par code
                        .requestMatchers(HttpMethod.POST, "/demandes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/inscriptions-paroisse").permitAll()
                        .requestMatchers(HttpMethod.POST, "/inscriptions-paroisse/otp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/demandes/code/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/demandes/code/*/type-paiement").permitAll()
                        .requestMatchers(HttpMethod.GET, "/facture/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/details-paiement/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/paiements/quote/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/paiements/checkout/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/webhooks/fedapay").permitAll()

                        // Catalogue public : uniquement les routes scopées (paroisse / type / actifs)
                        .requestMatchers(HttpMethod.GET, "/paroisses", "/paroisses/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doyennes", "/doyennes/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-demandes/paroisse/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forfait-tarifs/type-demande/*/actifs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horaires/paroisse/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horaires/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-paiement", "/type-paiement/*").permitAll()
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

                        // Exception au référentiel global : une paroisse tient ses propres
                        // coordonnées, son RIB et le logo du reçu.
                        .requestMatchers(HttpMethod.PATCH, "/paroisses/*/coordonnees")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/paroisses/*/logo")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/paroisses/*/logo")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/paroisses/*/logo")
                        .hasAnyRole("ADMIN", "SECRETAIRE", "CURE", "COMPTABLE_LOCAL", "SUPER_ADMIN")

                        // Gestion du référentiel global
                        .requestMatchers(HttpMethod.POST, "/paroisses", "/doyennes", "/type-paiement")
                        .access(globalSuperAdminAccess())
                        .requestMatchers(HttpMethod.PUT, "/paroisses/**", "/doyennes/**", "/type-paiement/**")
                        .access(globalSuperAdminAccess())
                        .requestMatchers(HttpMethod.DELETE, "/paroisses/**", "/doyennes/**", "/type-paiement/**")
                        .access(globalSuperAdminAccess())

                        // Paramétrage paroissial : admin local, ou comptable/super admin
                        // pour le catalogue plateforme (support is_system) cloné aux tenants.
                        .requestMatchers(HttpMethod.POST, "/horaires", "/type-demandes", "/forfait-tarifs")
                        .hasAnyRole("ADMIN", "COMPTABLE", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/horaires/**", "/type-demandes/**", "/forfait-tarifs/**")
                        .hasAnyRole("ADMIN", "COMPTABLE", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/horaires/**", "/type-demandes/**", "/forfait-tarifs/**")
                        .hasAnyRole("ADMIN", "COMPTABLE", "SUPER_ADMIN")

                        // Gestion des demandes après leur dépôt public
                        .requestMatchers(HttpMethod.PUT, "/demandes/**")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/demandes/*/intention")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/demandes/*/validation")
                        .hasAnyRole("CURE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/demandes/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Paiements : saisie par le secrétariat, suppression par un administrateur.
                        // La caisse locale (espèces) est encaissée ici, hors solde de reversement.
                        .requestMatchers(HttpMethod.POST, "/details-paiement", "/details-paiement/caisse/**")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/details-paiement/caisse/**")
                        .hasAnyRole("SECRETAIRE", "COMPTABLE_LOCAL", "ADMIN", "SUPER_ADMIN")
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

    private AuthorizationManager<RequestAuthorizationContext> swaggerAccess() {
        return (authenticationSupplier, context) -> {
            boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
            boolean swaggerEnabled = environment.getProperty(
                    "springdoc.swagger-ui.enabled", Boolean.class, false
            );
            return new AuthorizationDecision(!prod && swaggerEnabled);
        };
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
