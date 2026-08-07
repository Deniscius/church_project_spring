package com.eyram.dev.church_project_spring.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = splitCsv(allowedOrigins);
        List<String> patterns = splitCsv(allowedOriginPatterns);

        if (!origins.isEmpty()) {
            configuration.setAllowedOrigins(origins);
        }
        if (!patterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(patterns);
        }
        if (origins.isEmpty() && patterns.isEmpty()) {
            configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "ngrok-skip-browser-warning"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private static List<String> splitCsv(String raw) {
        return Arrays.stream(Optional.ofNullable(raw).orElse("").split(","))
                .map(value -> value != null ? value.trim() : "")
                .filter(StringUtils::hasText)
                .toList();
    }
}
