package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Called under the invoice/parish lock. Ambiguous POST outcomes remain reserved, never blindly
 * retried.
 */
@Service
@RequiredArgsConstructor
public class SasPayCheckoutStore {
    private final JdbcTemplate jdbc;
    private final SasPayClient client;

    public record Checkout(
            UUID id,
            String purpose,
            UUID target,
            int amount,
            int platformFee,
            String remoteId,
            String transactionId,
            String url,
            String state,
            String error) {
        public String reference() {
            return "SASPAY:" + remoteId;
        }
    }

    private final org.springframework.jdbc.core.RowMapper<Checkout> mapper =
            (r, n) ->
                    new Checkout(
                            r.getObject("id", UUID.class),
                            r.getString("purpose"),
                            r.getObject("target_id", UUID.class),
                            r.getInt("amount"),
                            r.getInt("platform_fee"),
                            r.getString("remote_id"),
                            r.getString("transaction_id"),
                            r.getString("payment_url"),
                            r.getString("state"),
                            r.getString("error_message"));

    public Checkout latest(String purpose, UUID target) {
        return jdbc
                .query(
                        "SELECT * FROM saspay_checkout WHERE purpose=? AND target_id=? ORDER BY"
                            + " created_at DESC,id DESC LIMIT 1",
                        mapper,
                        purpose,
                        target)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Checkout get(UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM saspay_checkout WHERE id=?" + (lock ? " FOR UPDATE" : ""),
                        mapper,
                        id)
                .stream()
                .findFirst()
                .orElseThrow();
    }

    public Checkout start(
            String purpose, UUID target, int amount, int platformFee, Map<String, Object> body) {
        var old = latest(purpose, target);
        if (old != null
                && Set.of("CREATING", "PENDING", "UNCERTAIN", "SUCCESS", "REVIEW")
                        .contains(old.state())) return old;
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO saspay_checkout(id,purpose,target_id,amount,platform_fee) VALUES"
                    + " (?,?,?,?,?)",
                id,
                purpose,
                target,
                amount,
                platformFee);
        body.put("metadata", Map.of("missanye_reference", id.toString()));
        try {
            JsonNode result = client.createCheckout(body);
            String remote = UUID.fromString(result.path("id").asText()).toString();
            jdbc.update(
                    "UPDATE saspay_checkout SET remote_id=? WHERE id=?",
                    UUID.fromString(remote),
                    id);
            String url = SasPayClient.checkoutUrl(result);
            jdbc.update(
                    "UPDATE saspay_checkout SET remote_id=?,payment_url=?,state='PENDING' WHERE"
                        + " id=?",
                    UUID.fromString(remote),
                    url,
                    id);
        } catch (Exception e) {
            boolean uncertain =
                    !(e instanceof SasPayClient.Unavailable rejected) || rejected.uncertain();
            String message =
                    e instanceof SasPayClient.Unavailable
                            ? e.getMessage()
                            : "Réponse SasPay incomplète. Vérification manuelle nécessaire.";
            jdbc.update(
                    "UPDATE saspay_checkout SET state=?,error_message=? WHERE id=?",
                    uncertain ? "UNCERTAIN" : "FAILED",
                    message,
                    id);
        }
        return get(id, false);
    }

    public List<Checkout> pending() {
        return jdbc.query(
                "SELECT * FROM saspay_checkout WHERE state IN ('PENDING','UNCERTAIN') AND remote_id"
                    + " IS NOT NULL AND (checked_at < CURRENT_TIMESTAMP - INTERVAL '30 seconds' OR"
                    + " EXISTS(SELECT 1 FROM saspay_webhook_event e WHERE"
                    + " e.received_at>saspay_checkout.checked_at)) ORDER BY checked_at LIMIT 10",
                mapper);
    }

    public void touch(UUID id) {
        jdbc.update("UPDATE saspay_checkout SET checked_at=CURRENT_TIMESTAMP WHERE id=?", id);
    }

    public void state(UUID id, String state) {
        jdbc.update(
                "UPDATE saspay_checkout SET state=?,checked_at=CURRENT_TIMESTAMP WHERE id=?",
                state,
                id);
    }

    public void paid(
            UUID id, String transaction, BigDecimal charged, BigDecimal net, BigDecimal fee) {
        jdbc.update(
                "UPDATE saspay_checkout SET"
                    + " state='SUCCESS',transaction_id=?,charged=?,net_amount=?,provider_fee=?,checked_at=CURRENT_TIMESTAMP"
                    + " WHERE id=?",
                UUID.fromString(transaction),
                charged,
                net,
                fee,
                id);
    }

    public void webhook(String transaction, String event) {
        jdbc.update(
                "INSERT INTO saspay_webhook_event(transaction_id,event_type) VALUES (?,?) ON"
                    + " CONFLICT DO NOTHING",
                UUID.fromString(transaction),
                event);
    }
}
