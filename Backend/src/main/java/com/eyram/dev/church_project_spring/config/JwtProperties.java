package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT signé (HS256) + cookie d'accès HttpOnly optionnellement chiffré (AES-GCM).
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("28800000") long expirationMs,
        @DefaultValue("MS_AT") String cookieName,
        @DefaultValue("false") boolean cookieSecure,
        @DefaultValue("Lax") String cookieSameSite,
        @DefaultValue("/") String cookiePath,
        /** Si true, la valeur du cookie est chiffrée (opaque dans DevTools). */
        @DefaultValue("true") boolean cookieEncrypt
) {
}
