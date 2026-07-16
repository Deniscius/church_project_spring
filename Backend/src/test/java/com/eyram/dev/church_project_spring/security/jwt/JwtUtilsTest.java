package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static final String TEST_SECRET =
            "c2VjcmV0S2V5c0hvdWxkQmVBdExlYXN0MjU2Qml0c0xvbmc=";

    @Test
    void generatedTokenIsValidAndContainsTheUsername() {
        JwtUtils jwtUtils = new JwtUtils(new JwtProperties(TEST_SECRET, 60_000));

        String token = jwtUtils.generateToken(userDetails());

        assertTrue(jwtUtils.validateToken(token));
        assertEquals("test.user", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    void malformedTokenIsRejected() {
        JwtUtils jwtUtils = new JwtUtils(new JwtProperties(TEST_SECRET, 60_000));

        assertFalse(jwtUtils.validateToken("not-a-jwt"));
    }

    @Test
    void missingSecretFailsFast() {
        JwtProperties properties = new JwtProperties(" ", 60_000);

        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(properties));
    }

    @Test
    void nonPositiveExpirationFailsFast() {
        JwtProperties properties = new JwtProperties(TEST_SECRET, 0);

        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(properties));
    }

    @Test
    void invalidBase64SecretFailsFast() {
        JwtProperties properties = new JwtProperties("not-base64", 60_000);

        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(properties));
    }

    private UserDetailsImpl userDetails() {
        return new UserDetailsImpl(
                UUID.randomUUID(),
                "Utilisateur Test",
                "test.user",
                42L,
                false,
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                true
        );
    }
}
