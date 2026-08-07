package com.eyram.dev.church_project_spring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting léger (fenêtre glissante en mémoire) pour les routes publiques
 * sensibles : login, OTP, dépôt de demande, consultation par code de suivi.
 * Suffisant pour une instance unique ; remplacer par un store partagé en multi-nœuds.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int DEFAULT_LIMIT = 60;
    private static final int AUTH_LIMIT = 20;
    private static final int TRACKING_LIMIT = 30;
    private static final int WRITE_LIMIT = 15;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = normalizedPath(request);
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        return resolveLimit(method, path) <= 0;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = normalizedPath(request);
        int limit = resolveLimit(request.getMethod(), path);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = clientKey(request) + "|" + bucket(request.getMethod(), path);
        long now = Instant.now().toEpochMilli();
        pruneOccasionally(now);

        Window window = windows.compute(clientKey, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int count = window.count.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));

        if (count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Trop de requêtes. Réessayez dans une minute.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static int resolveLimit(String method, String path) {
        if (path.startsWith("/auth/login")) {
            return AUTH_LIMIT;
        }
        if (path.startsWith("/inscriptions-paroisse/otp")) {
            return AUTH_LIMIT;
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/demandes")) {
            return WRITE_LIMIT;
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/inscriptions-paroisse")) {
            return WRITE_LIMIT;
        }
        if (path.startsWith("/demandes/code/")
                || path.startsWith("/facture/code-suivie/")
                || path.startsWith("/details-paiement/code-suivie/")
                || path.startsWith("/paiements/quote/")
                || path.startsWith("/paiements/checkout/")) {
            return TRACKING_LIMIT;
        }
        return -1;
    }

    private static String bucket(String method, String path) {
        if (path.startsWith("/auth/login")) return "auth";
        if (path.startsWith("/inscriptions-paroisse/otp")) return "otp";
        if ("POST".equalsIgnoreCase(method) && path.equals("/demandes")) return "demande-create";
        if ("POST".equalsIgnoreCase(method) && path.equals("/inscriptions-paroisse")) return "inscription";
        if (path.startsWith("/paiements/")) return "paiement-public";
        return "tracking";
    }

    private static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri.isEmpty() ? "/" : uri;
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void pruneOccasionally(long now) {
        if (windows.size() < 2_000 || (now % 97) != 0) {
            return;
        }
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Window> entry = it.next();
            if (now - entry.getValue().windowStartMs >= WINDOW_MS * 2) {
                it.remove();
            }
        }
    }

    private record Window(long windowStartMs, AtomicInteger count) {
    }
}
