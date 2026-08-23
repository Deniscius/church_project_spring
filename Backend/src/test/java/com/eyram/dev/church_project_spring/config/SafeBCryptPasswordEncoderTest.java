package com.eyram.dev.church_project_spring.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafeBCryptPasswordEncoderTest {

    private final SafeBCryptPasswordEncoder encoder = new SafeBCryptPasswordEncoder();

    @Test
    void acceptsPasswordsUpToSeventyTwoUtf8Bytes() {
        String password = "a".repeat(72);
        String encoded = assertDoesNotThrow(() -> encoder.encode(password));
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void rejectsEncodingBeyondBcryptByteLimit() {
        String password = "é".repeat(37);
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(password));
    }

    @Test
    void oversizedAuthenticationCandidateSimplyDoesNotMatch() {
        String encoded = encoder.encode("valid-password");
        assertFalse(encoder.matches("a".repeat(73), encoded));
    }
}
