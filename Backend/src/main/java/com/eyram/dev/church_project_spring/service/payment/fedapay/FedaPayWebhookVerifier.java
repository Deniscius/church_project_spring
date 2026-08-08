package com.eyram.dev.church_project_spring.service.payment.fedapay;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Vérifie X-FEDAPAY-SIGNATURE (même schéma que le SDK officiel : t=...,s=HMAC-SHA256).
 */
@Component
@RequiredArgsConstructor
public class FedaPayWebhookVerifier {

    private final FedaPayProperties properties;

    public void verify(String payload, String signatureHeader) {
        String secret = properties.getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessRuleException("Secret webhook FedaPay manquant (FEDAPAY_WEBHOOK_SECRET)");
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new BusinessRuleException("En-tête X-FEDAPAY-SIGNATURE manquant");
        }

        long timestamp = extractTimestamp(signatureHeader);
        List<String> signatures = extractSignatures(signatureHeader, "s");
        if (timestamp < 0 || signatures.isEmpty()) {
            throw new BusinessRuleException("Signature webhook FedaPay invalide");
        }

        int tolerance = Math.max(0, properties.getWebhookToleranceSeconds());
        long now = System.currentTimeMillis() / 1000L;
        if (tolerance > 0 && Math.abs(now - timestamp) > tolerance) {
            throw new BusinessRuleException("Webhook FedaPay hors délai (timestamp)");
        }

        String signedPayload = timestamp + "." + payload;
        String expected = hmacSha256Hex(signedPayload, secret.trim());
        boolean match = signatures.stream().anyMatch(sig -> MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8)
        ));
        if (!match) {
            throw new BusinessRuleException("Signature webhook FedaPay non reconnue");
        }
    }

    private static long extractTimestamp(String header) {
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "t".equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1].trim());
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static List<String> extractSignatures(String header, String scheme) {
        List<String> out = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && scheme.equals(kv[0])) {
                out.add(kv[1].trim());
            }
        }
        return out;
    }

    private static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de calculer HMAC webhook", ex);
        }
    }
}
