package com.eyram.dev.church_project_spring.context;

/**
 * TenantContext — Gestionnaire du tenant actif par requête
 *
 * Dans une application multi-tenant, plusieurs clients (appelés "tenants") partagent
 * la même application, mais chacun ne doit voir QUE ses propres données.
 * Pour savoir à quel tenant appartient une requête en cours, il faut stocker
 * cette information quelque part pendant toute la durée de la requête.
 *
 * C'est exactement le rôle de cette classe.
 *
 * ── Comment ça fonctionne ? ──────────────────────────────────────────────────
 *
 * Un serveur web traite plusieurs requêtes en même temps, chacune dans un "thread"
 * (un fil d'exécution) séparé. Cette classe utilise un ThreadLocal, c'est-à-dire
 * une variable dont chaque thread possède SA PROPRE COPIE, indépendante des autres.
 *
 * Concrètement :
 *
 *   Requête A (Tenant 1) → son thread voit tenantId = 1
 *   Requête B (Tenant 2) → son thread voit tenantId = 2
 *   Requête C (Tenant 3) → son thread voit tenantId = 3
 *
 * Ces trois valeurs coexistent en même temps sans jamais se mélanger.
 *
 * ── Cycle de vie dans une requête ───────────────────────────────────────────
 *
 *   1. Le filtre JWT authentifie l'utilisateur puis recharge ses accès en base
 *   2. Il appelle setCurrentTenant(tenantId) → la valeur est stockée
 *   3. Tout le code métier (services, repositories) peut appeler getCurrentTenant()
 *   4. À la fin de la requête, clear() est obligatoirement appelé pour effacer
 *      la valeur — sans ça, le thread réutilisé pourrait "contaminer" la
 *      requête suivante avec le mauvais tenantId (faille de sécurité !)
 *
 * ── Pourquoi c'est important ? ───────────────────────────────────────────────
 *
 * C'est le contexte technique de la requête, pas une source d'autorité.
 * La source de vérité reste la base de données ; le filtre Hibernate et les
 * services lisent ensuite cette valeur pour isoler les données du tenant.
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
