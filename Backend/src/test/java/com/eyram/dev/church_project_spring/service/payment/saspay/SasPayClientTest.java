package com.eyram.dev.church_project_spring.service.payment.saspay;

import static org.assertj.core.api.Assertions.*;

import com.eyram.dev.church_project_spring.config.SasPayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.*;

class SasPayClientTest {
    HttpServer server;
    SasPayProperties properties;
    SasPayClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        properties = new SasPayProperties();
        properties.setEnabled(true);
        properties.setApiKey("sk_test_simulated");
        properties.setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
        client = new SasPayClient(properties);
        server.start();
    }

    @AfterEach
    void close() {
        server.stop(0);
    }

    @Test void configurationMessagesDistinguishDisabledMissingAndRejectedKeys(){
        properties.setEnabled(false);assertThat(client.configurationProblem()).contains("SASPAY_ENABLED");
        properties.setEnabled(true);properties.setApiKey("");assertThat(client.configurationProblem()).contains("SASPAY_API_KEY");
        properties.setApiKey("sk_test_simulated");assertThat(client.configurationProblem()).isNull();
    }

    @Test
    void checkoutUsesServerAuthorizationDecimalStringsAndUnwrapsEnvelope() throws Exception {
        var body = new AtomicReference<String>();
        server.createContext(
                "/api/v1/checkout-sessions/",
                e -> {
                    assertThat(e.getRequestHeaders().getFirst("Authorization"))
                            .isEqualTo("Bearer sk_test_simulated");
                    assertThat(e.getRequestHeaders().getFirst("Idempotency-Key")).isNull();
                    body.set(
                            new String(
                                    e.getRequestBody().readAllBytes(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                    byte[] response =
                            "{\"success\":true,\"data\":{\"id\":\"da5819bc-2db3-40b1-a061-370aa75cf645\",\"checkout_url\":\"https://pay.saspay.me/checkout/test\"}}"
                                    .getBytes();
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(201, response.length);
                    e.getResponseBody().write(response);
                    e.close();
                });
        var data = client.createCheckout(Map.of("amount", "2500.00", "currency", "XOF"));
        assertThat(data.path("id").asText()).isNotBlank();
        assertThat(new ObjectMapper().readTree(body.get()).path("amount").isTextual()).isTrue();
        assertThat(SasPayClient.checkoutUrl(data)).isEqualTo("https://pay.saspay.me/checkout/test");
    }

    @Test
    void rejectedKeysAreNotRetriedAndProviderBodiesCannotLeak() {
        var count = new AtomicInteger();
        server.createContext(
                "/api/v1/checkout-sessions/",
                e -> {
                    count.incrementAndGet();
                    byte[] response =
                            "{\"error\":\"secret-provider-body-sk_live_sensitive\"}".getBytes();
                    e.getResponseHeaders().set("Content-Type", "application/json");
                    e.sendResponseHeaders(401, response.length);
                    e.getResponseBody().write(response);
                    e.close();
                });
        assertThatThrownBy(() -> client.createCheckout(Map.of()))
                .isInstanceOf(SasPayClient.Unavailable.class)
                .hasMessageNotContaining("sensitive");
        assertThatThrownBy(() -> client.createCheckout(Map.of()))
                .isInstanceOf(SasPayClient.Unavailable.class);
        assertThat(count.get()).isEqualTo(1);
        assertThat(client.configurationProblem()).contains("refusé").doesNotContain(properties.getApiKey());
        properties.setApiKey("sk_test_rotated");assertThat(client.configurationProblem()).isNull();
    }

    @Test
    void rateLimitBackoffAndAmbiguousCreationAreDistinguished() {
        var count = new AtomicInteger();
        server.createContext(
                "/api/v1/checkout-sessions/",
                e -> {
                    count.incrementAndGet();
                    e.sendResponseHeaders(429, -1);
                    e.close();
                });
        assertThatThrownBy(() -> client.createCheckout(Map.of()))
                .isInstanceOfSatisfying(
                        SasPayClient.Unavailable.class,
                        error -> assertThat(error.uncertain()).isFalse());
        assertThatThrownBy(() -> client.createCheckout(Map.of()))
                .isInstanceOf(SasPayClient.Unavailable.class);
        assertThat(count.get()).isEqualTo(1);
        server.removeContext("/api/v1/checkout-sessions/");
        properties.setApiKey("sk_test_second");
        client = new SasPayClient(properties);
        server.createContext(
                "/api/v1/checkout-sessions/",
                e -> {
                    e.sendResponseHeaders(503, -1);
                    e.close();
                });
        assertThatThrownBy(() -> client.createCheckout(Map.of()))
                .isInstanceOfSatisfying(
                        SasPayClient.Unavailable.class,
                        error -> {
                            assertThat(error.uncertain()).isTrue();
                            assertThat(error.getMessage()).contains("HTTP 503");
                        });
    }

    @Test
    void checkoutRedirectMustStayOnOfficialHost() throws Exception {
        var json = new ObjectMapper();
        for (String url :
                List.of(
                        "http://pay.saspay.me/checkout/x",
                        "https://evil.test/checkout/x",
                        "https://pay.saspay.me.evil.test/checkout/x",
                        "https://user@pay.saspay.me/checkout/x"))
            assertThatThrownBy(
                            () ->
                                    SasPayClient.checkoutUrl(
                                            json.createObjectNode().put("checkout_url", url)))
                    .isInstanceOf(SasPayClient.Unavailable.class);
    }
}
