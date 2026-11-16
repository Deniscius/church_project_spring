package com.eyram.dev.church_project_spring.service.payment.saspay;

import static org.assertj.core.api.Assertions.*;

import com.eyram.dev.church_project_spring.config.SasPayProperties;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class SasPayWebhookVerifierTest {
    private String signature(byte[] body, String stamp) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(
                new SecretKeySpec(
                        "test-signing-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((stamp + ".").getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void rawBodyAgeAndSignatureMustAllMatch() throws Exception {
        var properties = new SasPayProperties();
        properties.setEnabled(true);
        properties.setWebhookSecret("test-signing-secret");
        var verifier = new SasPayWebhookVerifier(properties);
        byte[] body =
                "{ \"event\": \"webhook.test\", \"data\": {\"texte\":\"é\"} }"
                        .getBytes(StandardCharsets.UTF_8);
        String time = "" + Instant.now().getEpochSecond(), signature = signature(body, time);
        assertThatCode(() -> verifier.verify(body, signature, time)).doesNotThrowAnyException();
        assertThatThrownBy(() -> verifier.verify("{}".getBytes(), signature, time))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        for (String stamp :
                java.util.List.of(
                        "" + (Instant.now().getEpochSecond() - 301),
                        "" + (Instant.now().getEpochSecond() + 301),
                        "not-a-time",
                        "999999999999999999999"))
            assertThatThrownBy(() -> verifier.verify(body, signature(body, stamp), stamp))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> verifier.verify(body, "0".repeat(64), time))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> verifier.verify(body, null, time))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
