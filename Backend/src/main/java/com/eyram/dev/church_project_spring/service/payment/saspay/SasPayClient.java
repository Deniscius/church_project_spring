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
@lombok.extern.slf4j.Slf4j
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
        return configurationProblem()==null;
    }

    public String configurationProblem() {
        if(!properties.isEnabled())return "Paiement SasPay désactivé : SASPAY_ENABLED doit être true sur le backend Render.";
        if(properties.getApiKey()==null||properties.getApiKey().isBlank())return "Clé API SasPay absente : renseignez SASPAY_API_KEY sur le backend Render, puis redéployez.";
        if(properties.getApiKey().equals(rejectedKey))return "SasPay a refusé la clé API ou son accès au marchand. Vérifiez la clé Sandbox PAYIN, la validation du marchand et redéployez après correction.";
        return null;
    }

    public void requireConfigured() {
        String problem=configurationProblem();
        if(problem!=null)throw new Unavailable(problem,false);
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void reportConfiguration() {
        String key=properties.getApiKey();
        String mode=key==null||key.isBlank()?"missing":key.startsWith("sk_test_")?"sandbox":key.startsWith("sk_live_")?"live":"unrecognized";
        log.info("SasPay configuration: enabled={}, apiKeyPresent={}, webhookSecretPresent={}, keyMode={}",properties.isEnabled(),key!=null&&!key.isBlank(),properties.getWebhookSecret()!=null&&!properties.getWebhookSecret().isBlank(),mode);
    }

    static void validateApiBase(URI base) {
        if (base.getUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null)
            throw new Unavailable("Configuration SasPay invalide.", false);
        if ("http".equals(base.getScheme())
                && java.util.Set.of("localhost", "127.0.0.1").contains(base.getHost())) return;
        if (!"https".equals(base.getScheme())
                || !java.util.Set.of("api.saspay.me", "sandbox-api.saspay.me").contains(base.getHost())
                || (base.getPort() != -1 && base.getPort() != 443))
            throw new Unavailable("Hôte API SasPay invalide.", false);
    }

    private JsonNode call(String path, Object body) {
        requireConfigured();
        if (System.currentTimeMillis() < retryAfter)
            throw new Unavailable(
                    "SasPay est temporairement sollicité. Réessayez plus tard.", false);
        URI base = URI.create(properties.getApiBaseUrl());
        validateApiBase(base);
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
            log.warn("SasPay API request failed: httpStatus={}, operation={}", status,
                    body == null ? "read" : "createCheckout");
            if (status == 401 || status == 403) {
                rejectedKey = properties.getApiKey();
                log.warn("SasPay API access rejected: httpStatus={}; check API key scope, environment and merchant validation",status);
            }
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
                        case 404 -> "Adresse API SasPay introuvable : vérifiez SASPAY_API_BASE_URL sur le backend.";
                        default -> "Le service SasPay est temporairement indisponible (HTTP " + status + ").";
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
