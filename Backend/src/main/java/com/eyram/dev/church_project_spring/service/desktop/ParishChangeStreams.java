package com.eyram.dev.church_project_spring.service.desktop;

import com.eyram.dev.church_project_spring.config.CacheConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Small invalidation hints only. Data stays on the normal authorized REST endpoints. */
@Service
@RequiredArgsConstructor
public class ParishChangeStreams implements org.springframework.context.SmartLifecycle {
  private final DesktopRevisionService revisions;
  private final CacheManager caches;
  private final Map<SseEmitter, Subscription> subscriptions = new ConcurrentHashMap<>();
  private volatile boolean running;

  @Override
  public void start() {
    running = true;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Override
  public synchronized void stop() {
    running = false;
    subscriptions.keySet().forEach(SseEmitter::complete);
    subscriptions.clear();
  }

  private static final class Subscription {
    final UUID parish;
    final Authentication authentication;
    volatile String revision;

    Subscription(UUID parish, Authentication authentication, String revision) {
      this.parish = parish;
      this.authentication = authentication;
      this.revision = revision;
    }
  }

  public synchronized SseEmitter open(UUID parish, Authentication authentication)
      throws java.io.IOException {
    if (!running)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    String revision = revisions.revision(parish); // Authorize before reserving resources.
    long count =
        subscriptions.values().stream()
            .filter(s -> s.authentication.getName().equals(authentication.getName()))
            .count();
    if (subscriptions.size() >= 128 || count >= 4)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    // Reconnect periodically through the JWT filter, including token expiry and changed roles.
    var emitter = new SseEmitter(60000L);
    emitter.onCompletion(() -> subscriptions.remove(emitter));
    emitter.onTimeout(
        () -> {
          subscriptions.remove(emitter);
          emitter.complete();
        });
    emitter.onError(error -> subscriptions.remove(emitter));
    subscriptions.put(emitter, new Subscription(parish, authentication, revision));
    try {
      emitter.send(SseEmitter.event().name("revision").reconnectTime(15000).data(revision));
    } catch (java.io.IOException e) {
      subscriptions.remove(emitter);
      throw e;
    }
    return emitter;
  }

  @Scheduled(
      fixedDelayString = "${app.sync.stream-delay-ms:15000}",
      scheduler = "parishStreamScheduler")
  public void publish() {
    if (!running) return;
    for (var entry : subscriptions.entrySet()) {
      var previous = SecurityContextHolder.getContext();
      try {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(entry.getValue().authentication);
        SecurityContextHolder.setContext(context);
        var subscription = entry.getValue();
        String revision = revisions.revision(subscription.parish);
        if (!revision.equals(subscription.revision)) {
          invalidate(subscription.parish);
          entry.getKey().send(SseEmitter.event().name("revision").data(revision));
          subscription.revision = revision;
        } else entry.getKey().send(SseEmitter.event().comment("keep-alive"));
      } catch (Exception unavailable) {
        subscriptions.remove(entry.getKey());
        entry.getKey().complete();
      } finally {
        SecurityContextHolder.setContext(previous);
      }
    }
  }

  private void invalidate(UUID parish) {
    for (String name :
        List.of(
            CacheConfig.DEMANDE_PAGES,
            CacheConfig.DASHBOARD_STATS,
            CacheConfig.DASHBOARD_PROGRAMMES)) {
      var cache = caches.getCache(name);
      if (cache instanceof CaffeineCache caffeine)
        caffeine
            .getNativeCache()
            .asMap()
            .keySet()
            .removeIf(key -> key.toString().contains(parish.toString()));
    }
  }
}
