package com.eyram.dev.church_project_spring.context;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Session;

/**
 * HibernateTenantFilterActivator — Activateur du filtre de données par tenant
 *
 * Cette classe fait le pont entre le contexte de sécurité (qui sait QUEL tenant
 * est connecté) et la base de données (qui doit FILTRER les données en conséquence).
 *
 * ── Le problème qu'elle résout ───────────────────────────────────────────────
 *
 * Sans cette classe, si le Tenant 2 fait une requête pour lister ses articles,
 * Hibernate retournerait TOUS les articles de TOUS les tenants confondus.
 * Il faudrait alors ajouter manuellement un filtre "WHERE tenant_id = 2" dans
 * chaque requête, chaque repository, chaque méthode — ce qui est risqué et
 * fastidieux.
 *
 * Cette classe active à la place un filtre global directement sur la session
 * Hibernate, qui s'applique automatiquement à TOUTES les requêtes SQL de
 * cette session, sans aucune modification des repositories.
 *
 * ── Comment ça fonctionne ? ──────────────────────────────────────────────────
 *
 *   1. Elle lit le tenantId courant depuis TenantContext
 *   2. Elle récupère la session Hibernate active (la "connexion" à la BDD)
 *   3. Elle active le filtre nommé "tenantFilter" sur cette session
 *   4. Hibernate injecte alors automatiquement :
 *
 *        WHERE tenant_id = 2   ← dans TOUTES les requêtes de cette session
 *
 * ── Prérequis ────────────────────────────────────────────────────────────────
 *
 * Pour que ce filtre fonctionne, chaque entité concernée doit être annotée
 * avec @FilterDef et @Filter côté JPA, pour déclarer que le filtre "tenantFilter"
 * existe et sur quelle colonne il s'applique.
 *
 * ── Ce que cette classe ne fait PAS ─────────────────────────────────────────
 *
 * Elle ne décide pas QUI est le tenant courant — c'est le rôle de TenantContext.
 * Elle se contente d'appliquer techniquement cette décision au niveau de la BDD.
 * C'est une séparation claire des responsabilités : identification d'un côté,
 * application de l'autre.
 */
public class HibernateTenantFilterActivator {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void activateFilter() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }
    }
}
