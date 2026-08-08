package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
import com.eyram.dev.church_project_spring.service.payment.FedaPayWebhookAsyncProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final FedaPayPaymentService fedaPayPaymentService;
    private final FedaPayWebhookAsyncProcessor webhookAsyncProcessor;

    @PostMapping("/fedapay")
    public ResponseEntity<Map<String, Object>> fedapay(
            @RequestHeader(value = "X-FEDAPAY-SIGNATURE", required = false) String signature,
            @RequestBody String payload
    ) {
        // Signature vérifiée en synchrone ; traitement métier hors thread HTTP.
        fedaPayPaymentService.verifyWebhookSignature(payload, signature);
        webhookAsyncProcessor.processAsync(payload, signature);
        return ResponseEntity.accepted().body(Map.of("received", true, "async", true));
    }
}
