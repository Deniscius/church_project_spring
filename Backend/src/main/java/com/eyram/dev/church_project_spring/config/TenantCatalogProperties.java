package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tenant-catalog")
public class TenantCatalogProperties {

    /**
     * Au démarrage, clone le catalogue plateforme vers les paroisses
     * commercialement engagées qui n'ont encore aucun type.
     */
    private boolean backfillOnStartup = true;

    public boolean isBackfillOnStartup() {
        return backfillOnStartup;
    }

    public void setBackfillOnStartup(boolean backfillOnStartup) {
        this.backfillOnStartup = backfillOnStartup;
    }
}
