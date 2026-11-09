package com.missanye.desktop;

import com.missanye.desktop.api.ApiClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class DesktopTest {
    @TempDir Path temp;
    @Test void clientRejectsRemotePlainHttpAndEmbeddedCredentials() {
        assertThrows(IllegalArgumentException.class, () -> new ApiClient("http://example.com"));
        assertThrows(IllegalArgumentException.class, () -> new ApiClient("https://user:password@example.com"));
    }
    @Test void loginUsesCookieAndLogoutSendsMutationHeader() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var sessionChecked = new AtomicBoolean(); var logoutChecked = new AtomicBoolean();
        server.createContext("/auth/login-multi-tenant", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!body.contains("username") || !body.contains("password")) { exchange.sendResponseHeaders(400,-1); exchange.close(); return; }
            exchange.getResponseHeaders().add("Set-Cookie", "MS_AT=test-session; Path=/; HttpOnly");
            byte[] response = "{\"user\":{\"username\":\"secretary\"}}".getBytes();
            exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.createContext("/auth/me", exchange -> {
            boolean cookie = String.valueOf(exchange.getRequestHeaders().getFirst("Cookie")).contains("MS_AT=test-session");
            sessionChecked.set(cookie);
            byte[] response = "{\"user\":{\"username\":\"secretary\"}}".getBytes();
            exchange.sendResponseHeaders(cookie ? 200 : 401,response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.createContext("/auth/logout", exchange -> {
            logoutChecked.set("XMLHttpRequest".equals(exchange.getRequestHeaders().getFirst("X-Requested-With")) && "POST".equals(exchange.getRequestMethod()));
            byte[] response="{}".getBytes(); exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new ApiClient("http://127.0.0.1:"+server.getAddress().getPort());
            assertEquals("secretary", client.login("secretary", "password").path("user").path("username").asText());
            assertTrue(sessionChecked.get()); client.logout(); assertTrue(logoutChecked.get());
        } finally { server.stop(0); }
    }
    @Test void unauthorizedLoginReportsStatus() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/auth/login-multi-tenant", e -> { e.sendResponseHeaders(401,-1); e.close(); }); server.start();
        try {
            var client = new ApiClient("http://127.0.0.1:"+server.getAddress().getPort());
            assertEquals(401, assertThrows(ApiClient.ApiException.class, () -> client.login("x","y")).status());
        } finally { server.stop(0); }
    }
}
