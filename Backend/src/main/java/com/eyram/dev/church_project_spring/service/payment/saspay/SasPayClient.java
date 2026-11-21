package com.eyram.dev.church_project_spring.service.payment.saspay;

import com.eyram.dev.church_project_spring.config.SasPayProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

  private record RejectedAccess(String key, String base, int status) {}

  private volatile RejectedAccess rejectedAccess;
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
    return configurationProblem() == null;
  }

  public String configurationProblem() {
    if (!properties.isEnabled())
      return "Paiement SasPay désactivé : SASPAY_ENABLED doit être true sur le backend Render.";
    String key = properties.getApiKey();
    if (key.isBlank())
      return "Clé API SasPay absente : renseignez SASPAY_API_KEY sur le backend Render, puis"
                 + " redéployez.";
    if (!(key.startsWith("sk_test_") && key.length() > 8
            || key.startsWith("sk_live_") && key.length() > 8)
        || key.codePoints()
            .anyMatch(
                c ->
                    Character.isWhitespace(c)
                        || Character.isISOControl(c)
                        || c == '\''
                        || c == '"'))
      return "Format de clé SasPay invalide : retirez les guillemets et espaces internes, puis"
                 + " vérifiez la clé sur Render.";
    try {
      URI base = URI.create(properties.getApiBaseUrl());
      validateApiBase(base);
      boolean sandbox = key.startsWith("sk_test_");
      if ("https".equals(base.getScheme())
          && sandbox != "sandbox-api.saspay.me".equals(base.getHost()))
        return "Environnement SasPay incohérent : utilisez SASPAY_API_BASE_URL=auto ou l'URL"
                   + " correspondant à la clé sandbox/live.";
    } catch (Exception invalid) {
      return "Adresse API SasPay invalide : vérifiez SASPAY_API_BASE_URL sur le backend.";
    }
    RejectedAccess rejected = rejectedAccess;
    if (rejected != null
        && key.equals(rejected.key())
        && properties.getApiBaseUrl().equals(rejected.base()))
      return accessMessage(rejected.status());
    return null;
  }

  private static String accessMessage(int status) {
    return status == 401
        ? "Authentification SasPay refusée (HTTP 401) : vérifiez la clé et son environnement, puis"
              + " redéployez après correction."
        : "Accès SasPay refusé (HTTP 403) : vérifiez le scope PAYIN ou BOTH, les droits et l'état"
              + " du marchand, puis redéployez après correction.";
  }

  public void requireConfigured() {
    String problem = configurationProblem();
    if (problem != null) throw new Unavailable(problem, false);
  }

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void reportConfiguration() {
    String key = properties.getApiKey();
    String mode =
        key.isBlank()
            ? "missing"
            : key.startsWith("sk_test_")
                ? "sandbox"
                : key.startsWith("sk_live_") ? "live" : "unrecognized";
    String host = "invalid";
    try {
      URI base = URI.create(properties.getApiBaseUrl());
      validateApiBase(base);
      host = base.getHost();
    } catch (Exception ignored) {
      /* Never log a malformed URL, which may contain credentials. */
    }
    log.info(
        "SasPay configuration: enabled={}, apiKeyPresent={}, webhookSecretPresent={}, keyMode={},"
            + " apiHost={}, configurationValid={}",
        properties.isEnabled(),
        !key.isBlank(),
        properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank(),
        mode,
        host,
        configurationProblem() == null);
  }

  static void validateApiBase(URI base) {
    if (base.getHost() == null
        || base.getUserInfo() != null
        || base.getRawQuery() != null
        || base.getRawFragment() != null
        || !"/api/v1".equals(base.getPath().replaceAll("/+$", "")))
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
      throw new Unavailable("SasPay est temporairement sollicité. Réessayez plus tard.", false);
    String requestKey = properties.getApiKey();
    String requestBase = properties.getApiBaseUrl();
    URI base = URI.create(requestBase);
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
            .baseUrl(requestBase + "/")
            .requestFactory(factory)
            .defaultHeader("Authorization", "Bearer " + requestKey)
            .build();
    try {
      JsonNode root =
          body == null
              ? client.get().uri(path).retrieve().body(JsonNode.class)
              : client
                  .post()
                  .uri(path)
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .body(body)
                  .retrieve()
                  .body(JsonNode.class);
      if (root == null || root.path("success").isBoolean() && !root.path("success").asBoolean())
        throw new Unavailable(
            "Réponse SasPay invalide. Vérifiez le paiement avant de réessayer.", body != null);
      return root.has("data") ? root.path("data") : root;
    } catch (RestClientResponseException e) {
      int status = e.getStatusCode().value();
      log.warn(
          "SasPay API request failed: httpStatus={}, operation={}",
          status,
          body == null ? "read" : "createCheckout");
      if (status == 401 || status == 403) {
        rejectedAccess = new RejectedAccess(requestKey, requestBase, status);
        log.warn(
            "SasPay API access rejected: httpStatus={}; check API key scope, environment and"
                + " merchant validation",
            status);
      }
      if (status == 429) retryAfter = System.currentTimeMillis() + 60000;
      String message =
          switch (status) {
            case 401, 403 -> accessMessage(status);
            case 400, 422 -> "SasPay a refusé les coordonnées ou la configuration du paiement.";
            case 409 -> "Une opération SasPay est déjà en cours. Vérifiez son statut.";
            case 429 -> "SasPay est temporairement sollicité. Réessayez plus tard.";
            case 404 ->
                "Adresse API SasPay introuvable : vérifiez SASPAY_API_BASE_URL sur le backend.";
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
          || !uri.getPath().startsWith("/checkout/")) throw new IllegalArgumentException();
      return uri.toString();
    } catch (Exception e) {
      throw new Unavailable("SasPay a renvoyé une adresse de paiement invalide.", true);
    }
  }
}
