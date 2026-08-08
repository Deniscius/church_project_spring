package com.eyram.dev.church_project_spring.config;

import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.tenant-catalog", name = "backfill-on-startup", havingValue = "true", matchIfMissing = true)
public class TenantCatalogBackfillRunner implements ApplicationRunner {

    private final TenantCatalogBootstrapService tenantCatalogBootstrapService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Backfill catalogue tenant depuis le catalogue plateforme…");
        tenantCatalogBootstrapService.backfillEmptyTenants();
    }
}
