package com.eyram.dev.church_project_spring.service.payment.fedapay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FedaPayWebhookVerifierTest {

    private static final String SECRET = "wh_test_ci_secret";
    private static final String PAYLOAD =
            "{\"name\":\"transaction.approved\",\"entity\":{\"id\":123,\"status\":\"approved\"}}";

    private FedaPayWebhookVerifier verifier;
    private FedaPayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FedaPayProperties();
        properties.setWebhookSecret(SECRET);
        properties.setWebhookToleranceSeconds(300);
        verifier = new FedaPayWebhookVerifier(properties);
    }

    @Test
    void acceptsValidSignature() {
        long timestamp = System.currentTimeMillis() / 1000L;
        String header = "t=" + timestamp + ",s=" + sign(timestamp, PAYLOAD, SECRET);

        assertDoesNotThrow(() -> verifier.verify(PAYLOAD, header));
    }

    @Test
    void rejectsTamperedPayload() {
        long timestamp = System.currentTimeMillis() / 1000L;
        String header = "t=" + timestamp + ",s=" + sign(timestamp, PAYLOAD, SECRET);

        assertThrows(
                BusinessRuleException.class,
                () -> verifier.verify(PAYLOAD + " ", header)
        );
    }

    @Test
    void rejectsExpiredSignature() {
        long timestamp = (System.currentTimeMillis() / 1000L) - 301;
        String header = "t=" + timestamp + ",s=" + sign(timestamp, PAYLOAD, SECRET);

        assertThrows(BusinessRuleException.class, () -> verifier.verify(PAYLOAD, header));
    }

    @Test
    void rejectsMissingWebhookSecret() {
        properties.setWebhookSecret("");

        assertThrows(
                BusinessRuleException.class,
                () -> verifier.verify(PAYLOAD, "t=1,s=invalid")
        );
    }

    private static String sign(long timestamp, String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(raw.length * 2);
            for (byte value : raw) {
                out.append(String.format("%02x", value));
            }
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
