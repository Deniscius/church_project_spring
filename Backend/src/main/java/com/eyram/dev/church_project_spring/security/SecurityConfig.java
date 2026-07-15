package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.context.HibernateTenantFilterActivator;
import com.eyram.dev.church_project_spring.security.jwt.AuthEntryPointJwt;
import com.eyram.dev.church_project_spring.security.jwt.AuthTokenFilter;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
    public HibernateTenantFilterActivator hibernateTenantFilterActivator() {
        return new HibernateTenantFilterActivator();
    }

    @Bean
    public AuthTokenFilter authTokenFilter(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService,
            HibernateTenantFilterActivator hibernateTenantFilterActivator) {
        return new AuthTokenFilter(jwtUtils, userDetailsService, hibernateTenantFilterActivator);
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

                        // Gestion des utilisateurs
                        .requestMatchers(HttpMethod.POST, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Gestion des demandes après leur dépôt public
                        .requestMatchers(HttpMethod.PUT, "/demandes/**")
                        .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/demandes/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Paramétrage système réservé au SUPER_ADMIN
                        .requestMatchers("/type-paiement", "/type-paiement/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/forfait-tarifs", "/forfait-tarifs/**").hasRole("SUPER_ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
