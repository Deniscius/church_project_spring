package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtUtils(JwtProperties properties) {
        Assert.hasText(properties.secret(),
                "La propriété jwt.secret doit contenir une clé Base64");
        Assert.isTrue(properties.expirationMs() > 0,
                "La propriété jwt.expiration-ms doit être supérieure à zéro");

        this.signingKey = createSigningKey(properties.secret());
        this.jwtExpirationMs = properties.expirationMs();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "La propriété jwt.secret doit être une clé Base64 valide d'au moins 256 bits",
                    exception
            );
        }
    }

    public String generateToken(UserDetailsImpl user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("tenantId", user.getTenantId())   // ← null si SUPER_ADMIN global
                .claim("isGlobal", user.isGlobal())      // ← true si accès total
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Vérifie la signature et l'expiration une seule fois, puis retourne le sujet.
     * Les exceptions JWT sont volontairement laissées au filtre d'authentification.
     */
    public String parseUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}
