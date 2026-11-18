package com.missanye.desktop;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.missanye.desktop.api.ApiClient;
import com.missanye.desktop.repository.LocalStore;
import com.missanye.desktop.service.SyncService;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class IncrementalSyncTest {
  @TempDir Path temp;
  final ObjectMapper json = new ObjectMapper();
  HttpServer server;
  String parish = TestRequests.PARISH;
  String first = "11111111-1111-4111-8111-111111111111",
      second = "22222222-2222-4222-8222-222222222222";

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/auth/me", e -> reply(e, 200, "{\"user\":{\"id\":\"user-1\"}}"));
    server.createContext(
        "/desktop/paroisses/" + parish + "/sync/state",
        e -> reply(e, 200, "{\"cursor\":20,\"catalogue\":\"catalogue-v1\"}"));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  void reply(HttpExchange e, int status, String body) throws java.io.IOException {
    byte[] bytes = body.getBytes();
    e.getResponseHeaders().set("Content-Type", "application/json");
    e.sendResponseHeaders(status, bytes.length);
    e.getResponseBody().write(bytes);
    e.close();
  }

  ApiClient client() {
    return new ApiClient("http://127.0.0.1:" + server.getAddress().getPort());
  }

  LocalStore store() throws Exception {
    return LocalStore.unlock(
        temp.resolve("sync.db"),
        "https://api.example/",
        "tester",
        "password-secret".toCharArray(),
        "user-1");
  }

  JsonNode row(String id, String intention) {
    return json.createObjectNode()
        .put("publicId", id)
        .put("paroissePublicId", parish)
        .put("intention", intention);
  }

  void prepare(LocalStore store) throws Exception {
    store.put(
        "demands:" + parish,
        json.createArrayNode().add(row(first, "old")).add(row(second, "deleted")));
    store.put("demand-cursor:" + parish, json.getNodeFactory().numberNode(10));
  }

  @Test
  void appliesUpdatesAndTombstonesWithoutDownloadingFullList() throws Exception {
    server.createContext(
        "/desktop/paroisses/" + parish + "/sync/changes",
        e -> {
          var d = json.createObjectNode().put("cursor", 20).put("hasMore", false);
          d.set("upserts", json.createArrayNode().add(row(first, "new")));
          d.putArray("deleted").add(second);
          reply(e, 200, d.toString());
        });
    server.start();
    try (var store = store()) {
      prepare(store);
      var rows = new SyncService(store, client()).refresh(parish);
      assertEquals(1, rows.size());
      assertEquals("new", rows.path(0).path("intention").asText());
      assertEquals(20, store.get("demand-cursor:" + parish).asLong());
    }
  }

  @Test
  void failureOnLaterPagePreservesBothSnapshotAndCursor() throws Exception {
    server.createContext(
        "/desktop/paroisses/" + parish + "/sync/changes",
        e -> {
          if (e.getRequestURI().getQuery().equals("after=10")) {
            var d = json.createObjectNode().put("cursor", 15).put("hasMore", true);
            d.set("upserts", json.createArrayNode().add(row(first, "new")));
            d.putArray("deleted");
            reply(e, 200, d.toString());
          } else reply(e, 503, "{}");
        });
    server.start();
    try (var store = store()) {
      prepare(store);
      assertThrows(
          ApiClient.ApiException.class, () -> new SyncService(store, client()).refresh(parish));
      assertEquals("old", store.get("demands:" + parish).path(0).path("intention").asText());
      assertEquals(10, store.get("demand-cursor:" + parish).asLong());
    }
  }

  @Test
  void bootstrapCapturesCursorBeforeFullDownload() throws Exception {
    server.createContext(
        "/demandes/paroisse/" + parish,
        e -> {
          var d = json.createObjectNode().put("last", true);
          d.set("content", json.createArrayNode().add(row(first, "initial")));
          reply(e, 200, d.toString());
        });
    server.start();
    try (var store = store()) {
      var rows = new SyncService(store, client()).refresh(parish);
      assertEquals(1, rows.size());
      assertEquals(20, store.get("demand-cursor:" + parish).asLong());
    }
  }

  @Test
  void crossTenantRowsCannotReplaceLocalData() throws Exception {
    server.createContext(
        "/desktop/paroisses/" + parish + "/sync/changes",
        e -> {
          var d = json.createObjectNode().put("cursor", 20).put("hasMore", false);
          d.putArray("upserts")
              .addObject()
              .put("publicId", first)
              .put("paroissePublicId", "another-parish");
          d.putArray("deleted");
          reply(e, 200, d.toString());
        });
    server.start();
    try (var store = store()) {
      prepare(store);
      assertThrows(
          java.io.IOException.class, () -> new SyncService(store, client()).refresh(parish));
      assertEquals(10, store.get("demand-cursor:" + parish).asLong());
    }
  }

  @Test
  void unchangedCatalogueIsNotDownloaded() throws Exception {
    server.start();
    try (var store = store()) {
      var catalogue = json.createObjectNode();
      catalogue.putArray("types");
      catalogue.putArray("horaires");
      catalogue.putArray("paiements");
      catalogue.putObject("forfaits");
      store.put("catalogue:" + parish, catalogue);
      store.put("catalogue-revision:" + parish, json.getNodeFactory().textNode("catalogue-v1"));
      assertDoesNotThrow(() -> new SyncService(store, client()).catalogue(parish));
    }
  }
}
