package com.eyram.dev.church_project_spring.service.payment;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import com.eyram.dev.church_project_spring.utils.BusinessCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Jetons opaques pour les URL de retour paiement (FedaPay) :
 * le code de suivi n'apparaît pas en clair dans la barre d'adresse.
 */
@Service
public class PaymentReturnTokenService {

    private static final long TTL_SECONDS = 3600;
    private final byte[] hmacKey;

    public PaymentReturnTokenService(JwtProperties jwtProperties) {
        Assert.hasText(jwtProperties.secret(), "jwt.secret requis pour les jetons de retour paiement");
        try {
            this.hmacKey = MessageDigest.getInstance("SHA-256")
                    .digest(("pay-return:" + jwtProperties.secret()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le service de jetons de retour", e);
        }
    }

    public String issue(String codeSuivie) {
        String code = BusinessCodeGenerator.normalizeDemandeTrackingCode(codeSuivie);
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Code de suivi invalide");
        }
        long exp = Instant.now().getEpochSecond() + TTL_SECONDS;
        String payload = code + "|" + exp;
        String body = encode(payload.getBytes(StandardCharsets.UTF_8));
        String sig = encode(sign(payload.getBytes(StandardCharsets.UTF_8)));
        return body + "." + sig;
    }

    public String resolve(String token) {
        if (!StringUtils.hasText(token) || !token.contains(".")) {
            throw new IllegalArgumentException("Jeton de retour invalide");
        }
        String[] parts = token.split("\\.", 2);
        byte[] payloadBytes = decode(parts[0]);
        byte[] expectedSig = sign(payloadBytes);
        byte[] actualSig = decode(parts[1]);
        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new IllegalArgumentException("Jeton de retour invalide");
        }
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] bits = payload.split("\\|", 2);
        if (bits.length != 2) {
            throw new IllegalArgumentException("Jeton de retour invalide");
        }
        long exp;
        try {
            exp = Long.parseLong(bits[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Jeton de retour invalide");
        }
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("Jeton de retour expiré");
        }
        return BusinessCodeGenerator.normalizeDemandeTrackingCode(bits[0]);
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de signer le jeton de retour", e);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            // Le décodeur JDK accepte plusieurs derniers caractères Base64URL
            // pouvant représenter les mêmes octets lorsque des bits de bourrage
            // sont inutilisés. Une ré-encodage canonique empêche qu'un jeton
            // textuellement altéré soit néanmoins accepté.
            if (!encode(decoded).equals(value)) {
                throw new IllegalArgumentException("Jeton de retour invalide");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Jeton de retour invalide", ex);
        }
    }
}
