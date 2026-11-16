package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.eyram.dev.church_project_spring.config.SasPayProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
@RequiredArgsConstructor
public class SasPayWebhookVerifier {
    private final SasPayProperties properties;

    public void verify(byte[] body, String signature, String timestamp) {
        if (!properties.isEnabled() || properties.getWebhookSecret().isBlank())
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Webhook SasPay non configuré");
        try {
            if (timestamp == null
                    || !timestamp.matches("[0-9]{1,12}")
                    || signature == null
                    || !signature.matches("[0-9a-f]{64}")) throw new IllegalArgumentException();
            long time = Long.parseLong(timestamp), now = Instant.now().getEpochSecond();
            if (time < now - 300 || time > now + 300) throw new IllegalArgumentException();
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(
                    new SecretKeySpec(
                            properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature)))
                throw new IllegalArgumentException();
        } catch (Exception invalid) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Signature SasPay invalide ou expirée");
        }
    }
}
