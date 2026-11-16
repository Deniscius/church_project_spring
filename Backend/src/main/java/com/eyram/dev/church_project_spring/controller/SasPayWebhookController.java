package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.service.payment.saspay.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class SasPayWebhookController {
    private final SasPayWebhookVerifier verifier;
    private final SasPayCheckoutStore store;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    @PostMapping("/webhooks/saspay")
    public Map<String, Boolean> receive(
            @RequestBody byte[] body,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Webhook-Event", required = false) String header)
            throws Exception {
        verifier.verify(body, signature, timestamp);
        var payload = json.readTree(body);
        String event = payload.path("event").asText();
        if (!event.equals(header))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Événement SasPay incohérent");
        if (Set.of(
                        "transaction.created",
                        "transaction.success",
                        "transaction.failed",
                        "transaction.cancelled")
                .contains(event)) {
            final String id;
            try {
                id = UUID.fromString(payload.path("data").path("id").asText()).toString();
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Référence SasPay manquante");
            }
            // ACK only after durable insertion. A bounded scheduled worker verifies the provider
            // status.
            transactions.executeWithoutResult(t -> store.webhook(id, event));
        }
        return Map.of("received", true);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> rejected(
            ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode())
                .body(
                        Map.of(
                                "message",
                                error.getReason() == null ? "Webhook refusé" : error.getReason()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(
            com.fasterxml.jackson.core.JsonProcessingException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> malformed() {
        return org.springframework.http.ResponseEntity.badRequest()
                .body(Map.of("message", "Notification SasPay invalide"));
    }
}
