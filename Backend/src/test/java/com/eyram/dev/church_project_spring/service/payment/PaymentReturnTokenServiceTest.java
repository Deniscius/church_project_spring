package com.eyram.dev.church_project_spring.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import org.junit.jupiter.api.Test;

class PaymentReturnTokenServiceTest {

    private final PaymentReturnTokenService service = new PaymentReturnTokenService(
            new JwtProperties(
                    "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                    "missanye-api",
                    "missanye-web",
                    28_800_000L,
                    "MS_AT",
                    false,
                    "Lax",
                    "/",
                    true
            )
    );

    @Test
    void issueCreatesOpaqueTokenThatResolvesToNormalizedTrackingCode() {
        String code = "ms-test-abcdefghij";

        String token = service.issue(code);

        assertFalse(token.contains(code));
        assertEquals("MS-TEST-ABCDEFGHIJ", service.resolve(token));
    }

    @Test
    void resolveRejectsTamperedToken() {
        String token = service.issue("MS-TEST-ABCDEFGHIJ");
        char replacement = token.endsWith("A") ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThrows(IllegalArgumentException.class, () -> service.resolve(tampered));
    }
}
