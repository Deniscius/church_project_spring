package com.eyram.dev.church_project_spring.security;

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

import com.eyram.dev.church_project_spring.security.jwt.AuthEntryPointJwt;
import com.eyram.dev.church_project_spring.security.jwt.AuthTokenFilter;
import com.eyram.dev.church_project_spring.security.jwt.JwtUtils;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthEntryPointJwt authEntryPointJwt;

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthTokenFilter authTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
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
                        // ✅ Public endpoints
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // ✅ Public endpoints : dépôt et consultation par code
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
                        
                        // ✅ Admin endpoints (ADMIN, SUPER_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        
                        // ✅ Secrétaire + Admin : gestion complète des demandes
                        .requestMatchers(HttpMethod.POST, "/demandes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/demandes/**").hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/demandes/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        
                        // ✅ Super Admin only : gestion système
                        .requestMatchers("/type-paiement").hasRole("SUPER_ADMIN")
                        .requestMatchers("/type-paiement/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/forfait-tarifs").hasRole("SUPER_ADMIN")
                        .requestMatchers("/forfait-tarifs/**").hasRole("SUPER_ADMIN")
                        
                        // ✅ Authentifiés : consultation générale
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
