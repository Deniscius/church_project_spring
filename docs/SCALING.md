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
- **Fichiers** : magic-bytes, logos redimensionnés/compressés (≤ 512 px), limites séparées, purge scans au rejet, Cache-Control

## Avant multi-pods (obligatoire)

1. **Redis** (ou équivalent) pour :
   - cache partagé (`spring.cache.type=redis`)
   - OTP inscription (aujourd’hui en mémoire JVM)
   - rate-limit distribué
2. **Stockage partagé** logos / pièces :
   - volume NFS/PVC **ou** S3/MinIO (garder `StoredFileService` comme façade)
   - `APP_STORAGE_ROOT` hors du pod éphémère
3. Reverse-proxy (nginx / Traefik) :
   - TLS, gzip, timeouts
   - restreindre `/actuator/**` au réseau interne
4. Postgres :
   - connexion pooling (PgBouncer) si > 2 pods
   - backups + monitoring connexions

## Stockage fichiers (bonnes pratiques)

| Asset | Limite | Traitement | Cache HTTP |
|-------|--------|------------|------------|
| Logo paroisse | 2 Mo entrée → JPEG/PNG optimisé | resize max 512 px, strip meta | `private, max-age=86400` |
| Mandat / CNI | 5 Mo | magic-bytes PDF/JPEG/PNG | `private, no-store` |

- Ne jamais committer `uploads/` (gitignore).
- Au **rejet** d’inscription : suppression des scans (PII).
- Prochaine étape prod : adaptateur S3 + URLs signées pour les pièces d’identité.

## Variables utiles

```bash
HIKARI_MAX_POOL_SIZE=30
TOMCAT_MAX_THREADS=80
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_PUBLIC=60
APP_RATE_LIMIT_DEMANDE=12
APP_RATE_LIMIT_AUTH=8
APP_STORAGE_ROOT=/var/lib/missanye/uploads
APP_STORAGE_MAX_LOGO_BYTES=2097152
APP_STORAGE_LOGO_MAX_SIDE=512
```

## Observabilité

- Health : `/actuator/health`
- Métriques : `/actuator/prometheus` → Grafana / Datadog
- Surveiller : Hikari active/pending, Tomcat busy, 429 rate-limit, latence webhook

## Front

- React Query : cache 5–30 min, pas de refetch au focus
- Routes lazy + chunks vendor
- Horaires publics : échantillon aléatoire côté UI (payload complet encore mis en cache API 5 min)
