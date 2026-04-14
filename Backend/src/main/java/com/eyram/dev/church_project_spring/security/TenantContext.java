package com.eyram.dev.church_project_spring.security;

import java.util.UUID;

/**
 * Contexte de tenant (paroisse) pour l'isolation des données.
 * Fournit un accès thread-safe au tenant courant.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> tenantId = new ThreadLocal<>();

    /**
     * Définit le tenant courant.
     */
    public static void setTenantId(UUID id) {
        tenantId.set(id);
    }

    /**
     * Récupère le tenant courant.
     */
    public static UUID getTenantId() {
        return tenantId.get();
    }

    /**
     * Vérifie si un tenant est défini.
     */
    public static boolean hasTenant() {
        return tenantId.get() != null;
    }

    /**
     * Réinitialise le tenant (à appeler après la requête).
     */
    public static void clear() {
        tenantId.remove();
    }

    /**
     * Exécute une tâche avec un tenant spécifique.
     */
    public static <T> T withTenant(UUID tenantId, TenantTask<T> task) {
        UUID previous = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            return task.execute();
        } finally {
            if (previous != null) {
                TenantContext.setTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Interface pour exécuter des tâches avec tenant.
     */
    @FunctionalInterface
    public interface TenantTask<T> {
        T execute();
    }
}
