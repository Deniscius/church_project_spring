# Montée en charge — Missanye

Checklist opérationnelle pour passer d’un nœud unique à une plateforme résiliente.

## Déjà en place (cette itération)

- Cache **Caffeine** avec TTL (doyennes, types paiement, horaires publics)
- **Rate limiting** IP sur endpoints publics (demandes, OTP, login, horaires, webhooks)
- Webhooks FedaPay : ACK HTTP rapide + traitement **async**
- Listes demandes paroisse **toujours paginées** (max 100 / page)
- Index SQL listes (`paroisse_id`, `created_at`, `statut_demande`)
- Actuator **Prometheus** (`/actuator/prometheus`)
- Pool Hikari / Tomcat **rééquilibrés** en prod (éviter threads ≫ connexions DB)
- Auth JWT **stateless** (cookie HttpOnly) — compatible multi-pods

## Avant multi-pods (obligatoire)

1. **Redis** (ou équivalent) pour :
   - cache partagé (`spring.cache.type=redis`)
   - OTP inscription (aujourd’hui en mémoire JVM)
   - rate-limit distribué
2. **Stockage partagé** logos / pièces :
   - volume NFS/PVC **ou** S3/MinIO (`app.storage.*`)
3. Reverse-proxy (nginx / Traefik) :
   - TLS, gzip, timeouts
   - restreindre `/actuator/**` au réseau interne
4. Postgres :
   - connexion pooling (PgBouncer) si > 2 pods
   - backups + monitoring connexions

## Variables utiles

```bash
HIKARI_MAX_POOL_SIZE=30
TOMCAT_MAX_THREADS=80
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_PUBLIC=60
APP_RATE_LIMIT_DEMANDE=12
APP_RATE_LIMIT_AUTH=8
```

## Observabilité

- Health : `/actuator/health`
- Métriques : `/actuator/prometheus` → Grafana / Datadog
- Surveiller : Hikari active/pending, Tomcat busy, 429 rate-limit, latence webhook

## Front

- React Query : cache 5–30 min, pas de refetch au focus
- Routes lazy + chunks vendor
- Horaires publics : échantillon aléatoire côté UI (payload complet encore mis en cache API 5 min)
