package com.eyram.dev.church_project_spring.service.payment.fedapay;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class FedaPayClient {

    private final FedaPayProperties properties;
    private final ObjectMapper objectMapper;

    private final Object clientLock = new Object();
    private volatile RestClient cachedClient;
    private volatile String cachedBaseUrl;
    private volatile String cachedSecretKey;
    private volatile int cachedConnectMs;
    private volatile int cachedReadMs;

    public CreatedTransaction createTransaction(CreateTransactionCommand command) {
        ensureEnabled();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", command.description());
        body.put("amount", command.amount());
        body.put("currency", Map.of("iso", properties.getCurrency()));
        if (StringUtils.hasText(command.callbackUrl())) {
            body.put("callback_url", command.callbackUrl());
        }
        if (command.metadata() != null && !command.metadata().isEmpty()) {
            body.put("custom_metadata", command.metadata());
        }
        body.put("customer", command.customer());

        JsonNode root = post("/transactions", body);
        JsonNode tx = unwrapTransaction(root);
        long id = requiredLong(tx, "id");
        String reference = textOrNull(tx, "reference");
        String status = textOrNull(tx, "status");
        return new CreatedTransaction(id, reference, status);
    }

    public PaymentToken generateToken(long transactionId) {
        ensureEnabled();
        JsonNode root = post("/transactions/" + transactionId + "/token", Map.of());
        String token = firstText(root, "token");
        String url = firstText(root, "url");
        if (!StringUtils.hasText(url)) {
            throw new BusinessRuleException("FedaPay n'a pas renvoyé d'URL de paiement");
        }
        return new PaymentToken(token, url);
    }

    public JsonNode getTransaction(long transactionId) {
        ensureEnabled();
        return unwrapTransaction(get("/transactions/" + transactionId));
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessRuleException("Paiement FedaPay désactivé (fedapay.enabled=false)");
        }
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new BusinessRuleException("Clé secrète FedaPay manquante (FEDAPAY_SECRET_KEY)");
        }
    }

    private JsonNode post(String path, Object body) {
        try {
            String raw = client().post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (RestClientResponseException ex) {
            log.error("FedaPay POST {} failed: {} {}", path, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessRuleException("Erreur FedaPay: " + summarizeError(ex));
        } catch (Exception ex) {
            log.error("FedaPay POST {} failed", path, ex);
            throw new BusinessRuleException("Impossible de contacter FedaPay");
        }
    }

    private JsonNode get(String path) {
        try {
            String raw = client().get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (RestClientResponseException ex) {
            log.error("FedaPay GET {} failed: {} {}", path, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessRuleException("Erreur FedaPay: " + summarizeError(ex));
        } catch (Exception ex) {
            log.error("FedaPay GET {} failed", path, ex);
            throw new BusinessRuleException("Impossible de contacter FedaPay");
        }
    }

    private RestClient client() {
        String baseUrl = properties.apiBaseUrl();
        String secret = properties.getSecretKey().trim();
        int connectMs = Math.max(500, properties.getConnectTimeoutMs());
        int readMs = Math.max(1000, properties.getReadTimeoutMs());

        RestClient local = cachedClient;
        if (local != null
                && Objects.equals(cachedBaseUrl, baseUrl)
                && Objects.equals(cachedSecretKey, secret)
                && cachedConnectMs == connectMs
                && cachedReadMs == readMs) {
            return local;
        }

        synchronized (clientLock) {
            if (cachedClient != null
                    && Objects.equals(cachedBaseUrl, baseUrl)
                    && Objects.equals(cachedSecretKey, secret)
                    && cachedConnectMs == connectMs
                    && cachedReadMs == readMs) {
                return cachedClient;
            }
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectMs))
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofMillis(readMs));

            cachedClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .defaultHeader("Authorization", "Bearer " + secret)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
            cachedBaseUrl = baseUrl;
            cachedSecretKey = secret;
            cachedConnectMs = connectMs;
            cachedReadMs = readMs;
            return cachedClient;
        }
    }

    private JsonNode unwrapTransaction(JsonNode root) {
        if (root == null || root.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (root.has("v1/transaction")) {
            return root.get("v1/transaction");
        }
        if (root.has("transaction")) {
            return root.get("transaction");
        }
        return root;
    }

    private static long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new BusinessRuleException("Réponse FedaPay invalide (id manquant)");
        }
        return value.asLong();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String firstText(JsonNode root, String field) {
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText();
        }
        JsonNode nested = root.path("v1/token");
        if (nested.has(field) && !nested.get(field).isNull()) {
            return nested.get(field).asText();
        }
        return null;
    }

    private static String summarizeError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            return ex.getStatusCode().toString();
        }
        return body.length() > 240 ? body.substring(0, 240) + "…" : body;
    }

    public record CreateTransactionCommand(
            String description,
            int amount,
            String callbackUrl,
            Map<String, Object> customer,
            Map<String, String> metadata
    ) {
    }

    public record CreatedTransaction(long id, String reference, String status) {
    }

    public record PaymentToken(String token, String url) {
    }
}
