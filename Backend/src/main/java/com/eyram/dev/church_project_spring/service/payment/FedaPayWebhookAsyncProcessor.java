package com.eyram.dev.church_project_spring.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Traitement asynchrone des webhooks FedaPay (ACK HTTP rapide).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FedaPayWebhookAsyncProcessor {

    private final FedaPayPaymentService fedaPayPaymentService;

    @Async("webhookExecutor")
    public void processAsync(String payload, String signatureHeader) {
        try {
            fedaPayPaymentService.handleWebhook(payload, signatureHeader);
        } catch (Exception ex) {
            log.error("Webhook FedaPay async en échec", ex);
        }
    }
}
