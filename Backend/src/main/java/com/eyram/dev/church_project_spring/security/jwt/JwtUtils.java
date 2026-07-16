package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.security.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

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

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.debug("Signature JWT invalide", e);
        } catch (MalformedJwtException e) {
            log.debug("JWT mal formé", e);
        } catch (ExpiredJwtException e) {
            log.debug("JWT expiré", e);
        } catch (UnsupportedJwtException e) {
            log.debug("JWT non supporté", e);
        } catch (IllegalArgumentException e) {
            log.debug("Jeton JWT vide ou invalide", e);
        }
        return false;
    }

}
