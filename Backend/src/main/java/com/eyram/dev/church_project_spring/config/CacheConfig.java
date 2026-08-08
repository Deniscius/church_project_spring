package com.eyram.dev.church_project_spring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache applicatif à TTL (Caffeine).
 * Prêt pour bascule Redis multi-pods via spring.cache.type=redis plus tard.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DOYENNES = "doyennes";
    public static final String TYPE_PAIEMENTS = "typePaiements";
    public static final String HORAIRES_PUBLIC_ACTIVES = "horairesPublicActives";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                caffeine(DOYENNES, 60, 32),
                caffeine(TYPE_PAIEMENTS, 60, 32),
                // Programme public : TTL court pour limiter la charge sans données trop figées.
                caffeine(HORAIRES_PUBLIC_ACTIVES, 5, 8)
        ));
        return manager;
    }

    private static CaffeineCache caffeine(String name, long expireMinutes, long maxSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build()
        );
    }
}
