package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Pose / lit le JWT d'accès dans un cookie HttpOnly.
 * La valeur peut être chiffrée (AES-GCM) pour qu'elle soit opaque dans DevTools.
 */
@Component
public class AuthCookieService {

    private static final String ENCRYPTION_PREFIX = "v1.";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final JwtProperties properties;
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthCookieService(JwtProperties properties) {
        this.properties = properties;
        Assert.hasText(properties.secret(), "jwt.secret requis pour le cookie d'auth");
        this.encryptionKey = deriveKey(properties.secret());
    }

    public void writeAccessCookie(HttpServletResponse response, String jwt) {
        String value = properties.cookieEncrypt() ? encrypt(jwt) : jwt;
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(value, Duration.ofMillis(properties.expirationMs())).toString());
    }

    public void clearAccessCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> properties.cookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .flatMap(this::decodeCookieValue);
    }

    public String cookieName() {
        return properties.cookieName();
    }

    private Optional<String> decodeCookieValue(String raw) {
        try {
            if (raw.startsWith(ENCRYPTION_PREFIX)) {
                return Optional.of(decrypt(raw.substring(ENCRYPTION_PREFIX.length())));
            }
            // Compat : ancienne valeur JWT brute (migration douce).
            return Optional.of(raw);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(properties.cookiePath())
                .maxAge(maxAge);

        String sameSite = properties.cookieSameSite() == null ? "Lax" : properties.cookieSameSite().trim();
        builder.sameSite(sameSite);
        return builder.build();
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return ENCRYPTION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de chiffrer le cookie d'authentification", e);
        }
    }

    private String decrypt(String encoded) throws Exception {
        byte[] all = Base64.getUrlDecoder().decode(encoded);
        byte[] iv = Arrays.copyOfRange(all, 0, GCM_IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(all, GCM_IV_LENGTH, all.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] plain = cipher.doFinal(cipherBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String jwtSecretBase64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(("cookie:" + jwtSecretBase64).getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de dériver la clé de chiffrement cookie", e);
        }
    }
}
