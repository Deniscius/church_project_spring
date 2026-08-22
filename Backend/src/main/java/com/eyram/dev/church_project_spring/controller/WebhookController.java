package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.payment.FedaPayPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final FedaPayPaymentService fedaPayPaymentService;

    @PostMapping("/fedapay")
    public ResponseEntity<Map<String, Object>> fedapay(
            @RequestHeader(value = "X-FEDAPAY-SIGNATURE", required = false) String signature,
            @RequestBody String payload
    ) {
        /*
         * Ne répondre avec succès qu'après le commit transactionnel. Si le
         * traitement échoue, la réponse 5xx permet au fournisseur de rejouer
         * l'événement au lieu de le perdre dans une file mémoire.
         */
        fedaPayPaymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
