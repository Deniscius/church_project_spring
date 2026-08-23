package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generatedTokenIsAcceptedForConfiguredIssuerAndAudience() {
        JwtUtils jwtUtils = jwtUtils("missanye-api", "missanye-web");

        String token = jwtUtils.generateToken(user("admin"));

        assertEquals("admin", jwtUtils.parseUsername(token));
    }

    @Test
    void tokenIsRejectedForAnotherIssuer() {
        String token = jwtUtils("missanye-api", "missanye-web")
                .generateToken(user("admin"));

        JwtUtils verifier = jwtUtils("another-api", "missanye-web");

        assertThrows(JwtException.class, () -> verifier.parseUsername(token));
    }

    @Test
    void tokenIsRejectedForAnotherAudience() {
        String token = jwtUtils("missanye-api", "missanye-web")
                .generateToken(user("admin"));

        JwtUtils verifier = jwtUtils("missanye-api", "another-client");

        assertThrows(JwtException.class, () -> verifier.parseUsername(token));
    }

    private static JwtUtils jwtUtils(String issuer, String audience) {
        return new JwtUtils(new JwtProperties(
                SECRET,
                issuer,
                audience,
                60_000,
                "MS_AT",
                false,
                "Lax",
                "/",
                true
        ));
    }

    private static UserDetailsImpl user(String username) {
        return new UserDetailsImpl(
                UUID.randomUUID(),
                "Administrateur",
                username,
                null,
                true,
                "unused",
                Set.of(),
                true
        );
    }
}
