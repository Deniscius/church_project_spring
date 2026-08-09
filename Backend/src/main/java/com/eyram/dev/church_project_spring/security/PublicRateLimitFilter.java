package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting léger en mémoire (fenêtre 1 minute / IP).
 * IP = RemoteAddr après ForwardedHeaderFilter (prod).
 * Pour multi-pods : Redis / API Gateway.
 */
@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public PublicRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer limit = resolveLimit(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + "|" + bucketName(request);
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= 60_000L) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int used = counter.count.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));

        if (used > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"message\":\"Trop de requêtes. Réessayez dans une minute.\"}");
            return;
        }

        if (counters.size() > 20_000) {
            counters.entrySet().removeIf(e -> now - e.getValue().windowStartMs > 120_000L);
        }

        filterChain.doFilter(request, response);
    }

    private Integer resolveLimit(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();

        if ("POST".equals(method) && path.contains("/webhooks/fedapay")) {
            return properties.webhookPerMinute();
        }
        if ("POST".equals(method) && (path.endsWith("/demandes") || path.endsWith("/demandes/"))) {
            return properties.demandeCreatePerMinute();
        }
        if ("POST".equals(method) && path.contains("/inscriptions-paroisse")
                && !path.contains("/otp")) {
            return properties.demandeCreatePerMinute();
        }
        if (("POST".equals(method) || "PATCH".equals(method)) && (
                path.contains("/auth/forgot-password")
                        || path.contains("/auth/reset-password")
                        || path.contains("/auth/login")
                        || path.contains("/demandes/suivi/par-telephone")
                        || path.contains("/inscriptions-paroisse/otp")
                        || path.contains("/paiements/checkout")
                        || path.contains("/paiements/retour")
                        || path.contains("/paiements/reconcile")
                        || (path.contains("/demandes/code/") && path.contains("/type-paiement"))
        )) {
            return properties.authSensitivePerMinute();
        }
        if (("GET".equals(method) || "PATCH".equals(method)) && (
                path.contains("/demandes/code/")
                        || path.contains("/facture/code-suivie/")
                        || path.contains("/details-paiement/code-suivie/")
                        || path.contains("/paiements/quote/")
        )) {
            return properties.codeLookupPerMinute();
        }
        if ("GET".equals(method) && (
                path.contains("/horaires/public")
                        || path.contains("/paroisses/public")
                        || path.contains("/paroisses/annuaire")
                        || path.contains("/doyennes")
                        || path.contains("/type-paiement")
        )) {
            return properties.publicPerMinute();
        }
        return null;
    }

    private static String bucketName(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod() == null ? "GET" : request.getMethod();
        if (path.contains("/webhooks/fedapay")) return "webhook";
        if (path.contains("/demandes") && "POST".equalsIgnoreCase(method) && !path.contains("/suivi")) {
            return "demande-create";
        }
        if (path.contains("/code/") || path.contains("/code-suivie/") || path.contains("/quote/")) {
            return "code-lookup";
        }
        if (path.contains("/forgot-password") || path.contains("/reset-password") || path.contains("/login")) {
            return "auth";
        }
        if (path.contains("/otp") || path.contains("/suivi/par-telephone")
                || path.contains("/checkout") || path.contains("/reconcile")) {
            return "sensitive";
        }
        return "public-read";
    }

    /**
     * Après {@code server.forward-headers-strategy=framework}, RemoteAddr est
     * déjà celui du client (proxy Render). On n'honore plus X-Forwarded-For brut
     * (spoofable si l'app est joignable hors proxy).
     */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record WindowCounter(long windowStartMs, AtomicInteger count) {
    }
}
