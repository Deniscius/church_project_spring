package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.eyram.dev.church_project_spring.config.SasPayProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** No implicit POST retries: hosted checkouts do not support Idempotency-Key. */
@Component
@RequiredArgsConstructor
public class SasPayClient {
    private final SasPayProperties properties;

    public static final class Unavailable extends BusinessRuleException {
        private final boolean uncertain;

        public Unavailable(String message, boolean uncertain) {
            super(message);
            this.uncertain = uncertain;
        }

        public boolean uncertain() {
            return uncertain;
        }
    }

    private volatile String rejectedKey;
    private volatile long retryAfter;

    public JsonNode createCheckout(Map<String, Object> body) {
        return call("checkout-sessions/", body);
    }

    public JsonNode status(String id) {
        UUID.fromString(id);
        return call("checkout-sessions/" + id + "/status/", null);
    }

    public JsonNode verify(String id) {
        UUID.fromString(id);
        return call("payments/" + id + "/verify/", null);
    }

    public JsonNode rates() {
        return call("pricing/my-rates/", null);
    }

    public boolean configured() {
        return properties.isEnabled()
                && !properties.getApiKey().isBlank()
                && !properties.getApiKey().equals(rejectedKey);
    }

    private JsonNode call(String path, Object body) {
        if (!configured())
            throw new Unavailable(
                    "Paiement SasPay indisponible : configuration du marchand nécessaire.", false);
        if (System.currentTimeMillis() < retryAfter)
            throw new Unavailable(
                    "SasPay est temporairement sollicité. Réessayez plus tard.", false);
        URI base = URI.create(properties.getApiBaseUrl());
        if (!"https".equals(base.getScheme())
                && !("http".equals(base.getScheme())
                        && java.util.Set.of("localhost", "127.0.0.1").contains(base.getHost())))
            throw new Unavailable("Configuration SasPay invalide.", false);
        if ("https".equals(base.getScheme()) && !"api.saspay.me".equals(base.getHost()))
            throw new Unavailable("Hôte API SasPay invalide.", false);
        var http =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        var client =
                RestClient.builder()
                        .baseUrl(properties.getApiBaseUrl().replaceAll("/+$", "") + "/")
                        .requestFactory(factory)
                        .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                        .build();
        try {
            JsonNode root =
                    body == null
                            ? client.get().uri(path).retrieve().body(JsonNode.class)
                            : client.post()
                                    .uri(path)
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .body(body)
                                    .retrieve()
                                    .body(JsonNode.class);
            if (root == null
                    || root.path("success").isBoolean() && !root.path("success").asBoolean())
                throw new Unavailable(
                        "Réponse SasPay invalide. Vérifiez le paiement avant de réessayer.",
                        body != null);
            return root.has("data") ? root.path("data") : root;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) rejectedKey = properties.getApiKey();
            if (status == 429) retryAfter = System.currentTimeMillis() + 60000;
            String message =
                    switch (status) {
                        case 401, 403 ->
                                "Accès SasPay refusé : vérifiez la clé et la validation du"
                                    + " marchand.";
                        case 400, 422 ->
                                "SasPay a refusé les coordonnées ou la configuration du paiement.";
                        case 409 -> "Une opération SasPay est déjà en cours. Vérifiez son statut.";
                        case 429 -> "SasPay est temporairement sollicité. Réessayez plus tard.";
                        default -> "Le service SasPay est temporairement indisponible.";
                    };
            throw new Unavailable(message, body != null && status >= 500);
        } catch (Unavailable e) {
            throw e;
        } catch (Exception e) {
            throw new Unavailable(
                    "Communication SasPay interrompue. Le statut doit être vérifié avant un nouvel"
                        + " essai.",
                    body != null);
        }
    }

    public static String checkoutUrl(JsonNode data) {
        try {
            var uri = URI.create(data.path("checkout_url").asText());
            if (!"https".equals(uri.getScheme())
                    || !"pay.saspay.me".equals(uri.getHost())
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1
                    || !uri.getPath().startsWith("/checkout/"))
                throw new IllegalArgumentException();
            return uri.toString();
        } catch (Exception e) {
            throw new Unavailable("SasPay a renvoyé une adresse de paiement invalide.", true);
        }
    }
}
