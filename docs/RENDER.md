# Déploiement Missanye sur Render

Architecture cible :

| Service | Type Render | Rôle |
|---------|-------------|------|
| `missanye-db` | PostgreSQL | Base Flyway / JPA |
| `missanye-api` | Web (Docker) | Spring Boot 3 / Java 17 |
| `missanye-web` | Static Site | React (Vite) |

Région : **Frankfurt (EU Central)** — plus proche du Togo que Oregon.  
Le blueprint IaC est à la racine : [`render.yaml`](../render.yaml).

## Formulaire Dashboard (1 service Docker = API seulement)

Si tu crées **missanye-api** à la main (pas le Blueprint) :

| Champ | Valeur |
|-------|--------|
| Name | `missanye-api` |
| Runtime | `Docker` |
| Branch | `master` |
| Region | `Frankfurt (EU Central)` — sinon Oregon |
| Root Directory | `Backend` |
| Dockerfile Path | `./Dockerfile` |
| Health check | `/actuator/health` |
| Disk (optionnel) | mount `/app/uploads` |

Ne laisse pas Root Directory vide : le Dockerfile est dans `Backend/`, pas à la racine.

Il reste à créer **Postgres** (`missanye-db`, même région) et le **static site** `missanye-web` (root `Frontend/church-frontend`). Le Blueprint fait les 3 d’un coup.

## Prérequis

1. Compte [Render](https://render.com) + dépôt GitHub/GitLab connecté.
2. Budget : Postgres `basic-256mb` + web `starter` (le free Postgres n’est plus fiable / souvent indisponible). Le static site reste gratuit.
3. Secrets prêts :
   - `JWT_SECRET` — Base64 ≥ 32 octets (`openssl rand -base64 32`)
   - Clés FedaPay live (si paiements activés)
   - URLs HTTPS des deux services (connues après le 1er deploy)

## Déploiement Blueprint (recommandé)

1. Push de la branche contenant `render.yaml`.
2. Render Dashboard → **New** → **Blueprint** → sélectionner le dépôt.
3. Apply. Les clés `sync: false` restent à renseigner.
4. Noter les URLs :
   - API : `https://missanye-api.onrender.com`
   - Front : `https://missanye-web.onrender.com`
5. Dans **missanye-api** → Environment :

   | Variable | Exemple |
   |----------|---------|
   | `JWT_SECRET` | `(openssl rand -base64 32)` |
   | `APP_CORS_ALLOWED_ORIGINS` | `https://missanye-web.onrender.com` |
   | `SPRING_MAIL_HOST` | `smtp-relay.brevo.com` (défini par le Blueprint) |
   | `SPRING_MAIL_PORT` | `587` (STARTTLS) |
   | `SPRING_MAIL_USERNAME` | identifiant SMTP fourni par Brevo |
   | `SPRING_MAIL_PASSWORD` | **clé SMTP Brevo**, jamais la clé API |
   | `APP_MAIL_FROM` | expéditeur vérifié dans Brevo (ex. `noreply@missanye.com`) |
   | `FEDAPAY_CALLBACK_BASE_URL` | `https://missanye-web.onrender.com/paiement` |
   | `FEDAPAY_ENABLED` | `true` (si go-live paiement) |
   | `FEDAPAY_SECRET_KEY` | `sk_live_…` |
   | `FEDAPAY_WEBHOOK_SECRET` | `wh_live_…` |

6. Dans **missanye-web** → Environment (rebuild requis) :

   | Variable | Exemple |
   |----------|---------|
   | `VITE_API_BASE_URL` | `https://missanye-api.onrender.com` |
   | `VITE_PUBLIC_SITE_URL` | `https://missanye-web.onrender.com` |

7. Dans Brevo → **Transactional** → **Settings** → **Configuration**, créer/copier les identifiants SMTP. Vérifier aussi l'expéditeur ou authentifier le domaine `missanye.com` (SPF/DKIM).
8. Redéployer **web** puis **api** (ou Manual Deploy).
9. Envoyer un e-mail transactionnel de test et vérifier son statut dans les journaux Brevo.
10. FedaPay Dashboard → webhook → `https://missanye-api.onrender.com/webhooks/fedapay`.

## Comportements importants

### `DATABASE_URL` → JDBC

Render injecte `postgres://…`. Au démarrage, `DatabaseUrlBootstrap` le convertit en `jdbc:postgresql://…?sslmode=require` et renseigne user/password Spring.

### Auth cookie / CSRF

`onrender.com` est un **public suffix** : `missanye-web` et `missanye-api` sont **cross-site**.

- `JWT_COOKIE_SECURE=true`
- `JWT_COOKIE_SAME_SITE=None`
- CORS = URL exacte du front (sans slash final)
- Mutations cookie : header `X-Requested-With: XMLHttpRequest` (envoyé par `apiClient`)

Avec domaines custom sur le même eTLD+1 (`www.missanye.com` + `api.missanye.com`), repasser `JWT_COOKIE_SAME_SITE=Lax`.

Le profil `prod` active aussi `server.forward-headers-strategy=framework` et `app.mail.fail-closed=true` (SMTP obligatoire).

### Aperçu PDF (reçus / feuilles)

L’aperçu utilise **PDF.js** (canvas) : plus d’iframe PDF native (souvent vide sur Render / Safari).

CSP du static site (`missanye-web`) doit autoriser au minimum :
`worker-src 'self' blob:; frame-src 'self' blob: data:; object-src 'self' blob: data:;`

Si le service a été créé **hors Blueprint**, recopier ces directives dans Dashboard → **Headers** (sinon l’ancien CSP reste actif même après un push `render.yaml`).

### Health check

`GET /actuator/health` est `permitAll` — utilisé pour le health check Render.

### Stockage fichiers

Sans disque monté, logos / scans sont **perdus** à chaque redeploy. Le blueprint monte `/app/uploads` (`APP_STORAGE_ROOT`, user non-root du Dockerfile). Pour multi-instances, prévoir S3/MinIO plus tard (voir `docs/SCALING.md`).

### Cold start (free / starter)

Les instances free s’endorment : premier hit lent. Préférer `starter` pour l’API en démo client.

### Flyway

Au premier boot sur base vide, Flyway applique les migrations. Ne pas activer `ddl-auto=update` en prod (`validate` uniquement).

## Déploiement manuel (sans Blueprint)

### API

- Runtime : Docker  
- Region : Frankfurt (même que la DB)  
- Root Directory : `Backend`  
- Dockerfile path : `./Dockerfile` (ou `Backend/Dockerfile` si Root est vide)  
- Context : `Backend`  
- Health : `/actuator/health`  
- Disk : `/app/uploads`  
- Env : `SPRING_PROFILES_ACTIVE=prod` + `DATABASE_URL` (Internal Database URL) + secrets ci-dessus.

### Front

- Static Site  
- Build : `npm ci && npm run build` (root = `Frontend/church-frontend`)  
- Publish : `dist`  
- Rewrite : `/*` → `/index.html`  
- Build env : `VITE_API_BASE_URL`, `VITE_PUBLIC_SITE_URL`

## Smoke test local Docker (API)

BuildKit recommandé (cache Maven `~/.m2`, layered JAR) :

```bash
cd Backend
DOCKER_BUILDKIT=1 docker build -t missanye-api .
docker run --rm -p 8081:8081 \
  -e PORT=8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="postgres://user:pass@host:5432/missanye" \
  -e JWT_SECRET="MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=" \
  -e APP_CORS_ALLOWED_ORIGINS="http://localhost:5173" \
  -e FEDAPAY_CALLBACK_BASE_URL="http://localhost:5173/paiement" \
  missanye-api
```

Options JVM : variable `JAVA_TOOL_OPTIONS` (pas de shell dans l’`ENTRYPOINT`).
Healthcheck image : `GET /actuator/health`.

## Checklist go-live

- [ ] `/actuator/health` → UP  
- [ ] Front charge et appelle l’API (pas d’erreur CORS)  
- [ ] Login admin : cookie `Secure` + `SameSite=None` (hébergement `*.onrender.com`)  
- [ ] Création demande publique + suivi  
- [ ] Webhook FedaPay (si enabled)  
- [ ] Upload logo paroisse survit à un redeploy (disque)  
- [ ] Domaines custom + TLS (optionnel) pointés vers les deux services  

## Domaines custom (optionnel)

1. Render → chaque service → **Custom Domains**.
2. Mettre à jour `APP_CORS_ALLOWED_ORIGINS`, `VITE_*`, `FEDAPAY_CALLBACK_BASE_URL` et le webhook FedaPay.
3. Rebuild du static site obligatoire après changement des `VITE_*`.
