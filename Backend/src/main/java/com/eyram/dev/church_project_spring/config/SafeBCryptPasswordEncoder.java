package com.eyram.dev.church_project_spring.config;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt n'utilise au maximum que 72 octets du mot de passe.
 * Cette variante refuse explicitement les encodages trop longs afin d'éviter
 * une troncature ambiguë ou une erreur interne selon l'opération appelée.
 */
public final class SafeBCryptPasswordEncoder extends BCryptPasswordEncoder {

    static final int MAX_PASSWORD_BYTES = 72;

    @Override
    public String encode(CharSequence rawPassword) {
        requireSupportedLength(rawPassword);
        return super.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (isTooLong(rawPassword)) {
            return false;
        }
        return super.matches(rawPassword, encodedPassword);
    }

    private static void requireSupportedLength(CharSequence rawPassword) {
        if (isTooLong(rawPassword)) {
            throw new IllegalArgumentException(
                    "Le mot de passe ne doit pas dépasser 72 octets encodés en UTF-8"
            );
        }
    }

    private static boolean isTooLong(CharSequence rawPassword) {
        return rawPassword != null
                && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
