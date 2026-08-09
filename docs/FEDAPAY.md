# FedaPay — configuration Missanye

L’intégration suit le flux **API serveur** FedaPay (pas de widget React).

Référence : [Authentification](https://docs.fedapay.com/integration-api/en/authentication-en) · [Transactions](https://docs.fedapay.com/integration-api/en/transactions-en) · [Webhooks](https://docs.fedapay.com/integration-api/en/webhooks-en)

## Clés du dashboard

| Clé dashboard | Variable Missanye | Usage |
|---------------|-------------------|--------|
| **Clé secrète** `sk_sandbox_…` / `sk_live_…` | `FEDAPAY_SECRET_KEY` | Backend uniquement |
| **Clé publique** | — | Non requise (checkout serveur) |
| **Secret webhook** `wh_sandbox_…` | `FEDAPAY_WEBHOOK_SECRET` | En-tête `X-FEDAPAY-SIGNATURE` |

Ne jamais committer les clés → `Backend/.env` (gitignoré).

## Configuration locale (`Backend/.env`)

```env
FEDAPAY_ENABLED=true
FEDAPAY_ENVIRONMENT=sandbox
FEDAPAY_SECRET_KEY=sk_sandbox_…
FEDAPAY_WEBHOOK_SECRET=wh_sandbox_…
FEDAPAY_CALLBACK_BASE_URL=http://localhost:5173/paiement
FEDAPAY_CUSTOMER_COUNTRY=tg
```

`FEDAPAY_CALLBACK_BASE_URL` pointe vers le **frontend**. Missanye ajoute `/retour?r=<jeton opaque>`.

## Flux conforme FedaPay

```
Fidèle                Backend Missanye              FedaPay
  |                         |                          |
  |-- quote --------------->|                          |
  |-- checkout ------------>|-- POST /transactions --->|
  |                         |-- POST .../token ------->|
  |<- paymentUrl -----------|                          |
  |-------- redirect browser ------------------------->|
  |                         |                          |
  |<- callback_url ?r=&id=&status= --------------------|
  |-- POST /paiements/retour/resoudre ---------------->|
  |   (resolve token + GET /transactions/:id)          |
  |                         |                          |
  |                         |<- webhook signed ---------|
  |                         |   transaction.approved   |
```

### Règles métier

1. **Création** : `POST /paiements/checkout/{code}` → create transaction + generate token → `paymentUrl`.
2. **Réutilisation** : si une TX `EN_ATTENTE` existe, le backend fait un **GET** FedaPay :
   - `approved` / `transferred` → marque `PAYE` (idempotent)
   - `declined` / `canceled` / `expired` → marque `ECHOUE`, puis nouvelle TX
   - sinon → réutilise l’URL ou régénère le token (**pas** de double `create`)
3. **Retour navigateur** : `callback_url` → `/paiement/retour?r=…` (+ `id` / `status` FedaPay).  
   Le front appelle `POST /paiements/retour/resoudre` avec `{ token, providerTransactionId }`.  
   Le query `status` **n’est pas** une source de vérité.
4. **Confirmation** : webhook `POST /webhooks/fedapay` (signé) **et** réconciliation GET au retour.  
   En local sans tunnel webhook, le GET au retour suffit souvent pour confirmer.

### Endpoints publics utiles

| Méthode | Chemin | Rôle |
|---------|--------|------|
| GET | `/paiements/quote/{code}` | Devis frais |
| POST | `/paiements/checkout/{code}` | Session FedaPay |
| POST | `/paiements/retour/resoudre` | Jeton opaque + reconcile |
| POST | `/paiements/reconcile/{code}` | Resync statut |
| POST | `/webhooks/fedapay` | Webhook signé |

## Webhook

1. Dashboard → **Webhooks** → endpoint `https://<api>/webhooks/fedapay`
2. Secret → `FEDAPAY_WEBHOOK_SECRET`
3. Événements : `transaction.approved`, `transaction.declined`, `transaction.canceled`

Local : `npm run prod:share` puis `ngrok http 4173`. Mettre `FEDAPAY_CALLBACK_BASE_URL=https://xxxx.ngrok-free.app/paiement` et, pour le webhook API, un second tunnel ou une URL publique vers `:8081/webhooks/fedapay`.

## Ne pas installer `fedapay-reactjs`

Peer React 18 uniquement ; Missanye est en React 19. Le checkout est une **redirection** vers l’URL FedaPay renvoyée par l’API.

## Prod

```env
FEDAPAY_ENABLED=true
FEDAPAY_ENVIRONMENT=live
FEDAPAY_SECRET_KEY=sk_live_…
FEDAPAY_WEBHOOK_SECRET=wh_live_…
FEDAPAY_CALLBACK_BASE_URL=https://www.missanye.com/paiement
```

Ne jamais mélanger clés sandbox et live.
