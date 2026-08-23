package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.security.jwt.AuthCookieService;
import com.eyram.dev.church_project_spring.security.jwt.AuthEntryPointJwt;
import com.eyram.dev.church_project_spring.security.jwt.AuthTokenFilter;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
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
            AuthCookieService authCookieService,
            UserDetailsService userDetailsService) {
        return new AuthTokenFilter(jwtUtils, authCookieService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            DaoAuthenticationProvider authenticationProvider,
            AuthTokenFilter authTokenFilter,
            PublicRateLimitFilter publicRateLimitFilter,
            CookieAuthMutationGuardFilter cookieAuthMutationGuardFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.contentTypeOptions(contentType -> {});
                    headers.frameOptions(frame -> frame.sameOrigin());
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                    ));
                    headers.addHeaderWriter(new StaticHeadersWriter(
                            "Permissions-Policy",
                            "camera=(), microphone=(), geolocation=()"
                    ));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "frame-ancestors 'self'; base-uri 'self'; form-action 'self'"
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
                        .requestMatchers("/auth/login", "/auth/login-multi-tenant",
                                "/auth/forgot-password", "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .access(swaggerAccess())
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**")
                        .hasAuthority(Permission.SYSTEM_ADMIN.authority())
                        .requestMatchers("/error", "/login", "/login.html", "/health-ui", "/health.html", "/assets/**", "/").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Dépôt et suivi publics
                        .requestMatchers(HttpMethod.POST, "/demandes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/inscriptions-paroisse").permitAll()
                        .requestMatchers(HttpMethod.POST, "/inscriptions-paroisse/otp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/demandes/code/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/demandes/code/*/type-paiement").permitAll()
                        .requestMatchers(HttpMethod.GET, "/facture/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/details-paiement/code-suivie/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/paiements/quote/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/paiements/checkout/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/paiements/retour/resoudre").permitAll()
                        .requestMatchers(HttpMethod.POST, "/paiements/reconcile/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/webhooks/fedapay").permitAll()

                        // Catalogue public
                        .requestMatchers(HttpMethod.GET, "/paroisses/public", "/paroisses/annuaire",
                                "/paroisses/annuaire/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doyennes", "/doyennes/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-demandes/paroisse/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forfait-tarifs/type-demande/*/actifs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horaires/paroisse/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/horaires/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/type-paiement", "/type-paiement/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/plans-saas/public").permitAll()
                        .requestMatchers(HttpMethod.POST, "/demandes/suivi/par-telephone").permitAll()
                        .requestMatchers(HttpMethod.POST, "/demandes/suivi/par-telephone/verifier").permitAll()

                        // Utilisateurs
                        .requestMatchers("/admin/users", "/admin/users/**")
                        .hasAuthority(Permission.USER_MANAGE.authority())
                        .requestMatchers(HttpMethod.POST, "/users")
                        .hasAuthority(Permission.USER_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/users/**")
                        .hasAuthority(Permission.USER_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/users/**")
                        .hasAuthority(Permission.USER_MANAGE.authority())
                        .requestMatchers(HttpMethod.GET, "/users")
                        .hasAuthority(Permission.USER_MANAGE.authority())

                        // Administration globale : permission + compte global obligatoires.
                        .requestMatchers("/paroisse-access", "/paroisse-access/**")
                        .access(globalPermissionAccess(Permission.PARISH_ACCESS_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/plans-saas")
                        .access(globalPermissionAccess(Permission.SAAS_PLAN_READ))
                        .requestMatchers(HttpMethod.POST, "/plans-saas")
                        .access(globalPermissionAccess(Permission.SAAS_PLAN_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/plans-saas/**")
                        .access(globalPermissionAccess(Permission.SAAS_PLAN_MANAGE))

                        // Paramètres de la paroisse courante
                        .requestMatchers(HttpMethod.PATCH, "/paroisses/*/coordonnees")
                        .hasAuthority(Permission.PARISH_SETTINGS_MANAGE.authority())
                        .requestMatchers(HttpMethod.POST, "/paroisses/*/logo")
                        .hasAuthority(Permission.PARISH_SETTINGS_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/paroisses/*/logo")
                        .hasAuthority(Permission.PARISH_SETTINGS_MANAGE.authority())
                        .requestMatchers(HttpMethod.GET, "/paroisses/*/logo")
                        .hasAuthority(Permission.PROFILE_READ.authority())

                        // Référentiels globaux
                        .requestMatchers(HttpMethod.POST, "/paroisses")
                        .access(globalPermissionAccess(Permission.PARISH_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/paroisses/**")
                        .access(globalPermissionAccess(Permission.PARISH_MANAGE))
                        .requestMatchers(HttpMethod.DELETE, "/paroisses/**")
                        .access(globalPermissionAccess(Permission.PARISH_MANAGE))
                        .requestMatchers(HttpMethod.POST, "/doyennes")
                        .access(globalPermissionAccess(Permission.DEANERY_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/doyennes/**")
                        .access(globalPermissionAccess(Permission.DEANERY_MANAGE))
                        .requestMatchers(HttpMethod.DELETE, "/doyennes/**")
                        .access(globalPermissionAccess(Permission.DEANERY_MANAGE))
                        .requestMatchers(HttpMethod.POST, "/type-paiement")
                        .access(globalPermissionAccess(Permission.PAYMENT_TYPE_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/type-paiement/**")
                        .access(globalPermissionAccess(Permission.PAYMENT_TYPE_MANAGE))
                        .requestMatchers(HttpMethod.DELETE, "/type-paiement/**")
                        .access(globalPermissionAccess(Permission.PAYMENT_TYPE_MANAGE))

                        // Catalogue paroissial
                        .requestMatchers(HttpMethod.POST, "/horaires")
                        .hasAuthority(Permission.SCHEDULE_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/horaires/**")
                        .hasAuthority(Permission.SCHEDULE_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/horaires/**")
                        .hasAuthority(Permission.SCHEDULE_MANAGE.authority())
                        .requestMatchers(HttpMethod.POST, "/type-demandes")
                        .hasAuthority(Permission.REQUEST_TYPE_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/type-demandes/**")
                        .hasAuthority(Permission.REQUEST_TYPE_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/type-demandes/**")
                        .hasAuthority(Permission.REQUEST_TYPE_MANAGE.authority())
                        .requestMatchers(HttpMethod.POST, "/forfait-tarifs")
                        .hasAuthority(Permission.PRICING_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/forfait-tarifs/**")
                        .hasAuthority(Permission.PRICING_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/forfait-tarifs/**")
                        .hasAuthority(Permission.PRICING_MANAGE.authority())

                        // Demandes
                        .requestMatchers(HttpMethod.PUT, "/demandes/**")
                        .hasAuthority(Permission.DEMAND_EDIT.authority())
                        .requestMatchers(HttpMethod.PATCH, "/demandes/*/intention")
                        .hasAuthority(Permission.DEMAND_EDIT.authority())
                        .requestMatchers(HttpMethod.PATCH, "/demandes/*/validation")
                        .hasAuthority(Permission.DEMAND_VALIDATE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/demandes/**")
                        .hasAuthority(Permission.DEMAND_DELETE.authority())

                        // Célébration
                        .requestMatchers(HttpMethod.POST, "/celebrations/dates/*/marquer-celebree")
                        .hasAuthority(Permission.CELEBRATION_MANAGE.authority())

                        // Paiements
                        .requestMatchers(HttpMethod.POST, "/details-paiement", "/details-paiement/caisse/**")
                        .hasAuthority(Permission.PAYMENT_MANAGE.authority())
                        .requestMatchers(HttpMethod.GET, "/details-paiement/caisse/**")
                        .hasAuthority(Permission.PAYMENT_READ.authority())
                        .requestMatchers(HttpMethod.PUT, "/details-paiement/**")
                        .hasAuthority(Permission.PAYMENT_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/details-paiement/**")
                        .hasAuthority(Permission.PAYMENT_DELETE.authority())

                        // Factures
                        .requestMatchers(HttpMethod.POST, "/facture")
                        .hasAuthority(Permission.INVOICE_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/facture/**")
                        .hasAuthority(Permission.INVOICE_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/facture/**")
                        .hasAuthority(Permission.INVOICE_MANAGE.authority())

                        // Corrections administratives des dates d'une demande
                        .requestMatchers(HttpMethod.POST, "/demande-dates")
                        .hasAuthority(Permission.DEMAND_DATE_MANAGE.authority())
                        .requestMatchers(HttpMethod.PUT, "/demande-dates/**")
                        .hasAuthority(Permission.DEMAND_DATE_MANAGE.authority())
                        .requestMatchers(HttpMethod.DELETE, "/demande-dates/**")
                        .hasAuthority(Permission.DEMAND_DATE_MANAGE.authority())

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(publicRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieAuthMutationGuardFilter, UsernamePasswordAuthenticationFilter.class)
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

    /**
     * Une authority globale n'est valable que pour un principal explicitement global.
     * Cela empêche un compte local possédant par erreur un rôle puissant d'agir sur
     * l'ensemble des tenants.
     */
    private AuthorizationManager<RequestAuthorizationContext> globalPermissionAccess(Permission permission) {
        return (authenticationSupplier, context) -> {
            var authentication = authenticationSupplier.get();
            boolean hasPermission = authentication.getAuthorities().stream()
                    .anyMatch(authority -> permission.authority().equals(authority.getAuthority()));
            boolean isGlobal = authentication.getPrincipal() instanceof UserDetailsImpl principal
                    && principal.isGlobal();

            return new AuthorizationDecision(
                    authentication.isAuthenticated() && hasPermission && isGlobal
            );
        };
    }
}
