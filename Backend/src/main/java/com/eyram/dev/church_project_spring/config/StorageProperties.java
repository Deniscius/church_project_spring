package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Racine locale des fichiers (scans inscription, logos).
     * En prod, monter un volume ou pointer vers un stockage partagé.
     */
    private String root = "./uploads";

    /** Taille max par fichier (octets). Défaut 5 Mo. */
    private long maxFileBytes = 5L * 1024 * 1024;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }
}
