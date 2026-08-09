package com.eyram.dev.church_project_spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Racine locale des fichiers (scans inscription, logos).
     * En prod, monter un volume partagé ou brancher un adaptateur S3/MinIO.
     */
    private String root = "./uploads";

    /** Taille max scans d'inscription (mandat / CNI). Défaut 5 Mo. */
    private long maxFileBytes = 5L * 1024 * 1024;

    /** Taille max logo avant optimisation. Défaut 2 Mo. */
    private long maxLogoBytes = 2L * 1024 * 1024;

    /** Plus grand côté (px) après redimensionnement logo. */
    private int logoMaxSidePx = 512;

    /** Qualité JPEG logo (0–1). */
    private float logoJpegQuality = 0.85f;

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

    public long getMaxLogoBytes() {
        return maxLogoBytes;
    }

    public void setMaxLogoBytes(long maxLogoBytes) {
        this.maxLogoBytes = maxLogoBytes;
    }

    public int getLogoMaxSidePx() {
        return logoMaxSidePx;
    }

    public void setLogoMaxSidePx(int logoMaxSidePx) {
        this.logoMaxSidePx = logoMaxSidePx;
    }

    public float getLogoJpegQuality() {
        return logoJpegQuality;
    }

    public void setLogoJpegQuality(float logoJpegQuality) {
        this.logoJpegQuality = logoJpegQuality;
    }
}
