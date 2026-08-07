package com.eyram.dev.church_project_spring.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DOYENNES = "doyennes";
    public static final String TYPE_PAIEMENTS = "typePaiements";
    public static final String HORAIRES_PUBLIC_ACTIVES = "horairesPublicActives";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(DOYENNES, TYPE_PAIEMENTS, HORAIRES_PUBLIC_ACTIVES);
    }
}
