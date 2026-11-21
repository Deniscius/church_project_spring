# SasPay dans Missanye

Les nouveaux paiements de demandes et d’abonnements utilisent le checkout hébergé SasPay. Les transactions historiques FedaPay gardent leurs références, montants et reçus. Leur ancien webhook et le client distant ont été retirés après un comptage nul des paiements en attente en production. Aucun nouveau checkout historique n’est créé. Les espèces et les reversements manuels restent inchangés.

## État initial

L’encaissement SasPay est **désactivé par défaut**. L’intégration se teste sans compte avec PostgreSQL et un serveur HTTP simulé. Un compte marchand SasPay et ses secrets sont nécessaires pour valider le véritable environnement sandbox. Aucune clé n’est embarquée dans le site ou Desktop.

Depuis Desktop, le paiement utilise l’e-mail déjà enregistré sur la demande. Pour une ancienne demande sans e-mail, ouvrir sa page de paiement sur le site et renseigner l’e-mail du payeur avant de continuer. Aucune clé SasPay n’est nécessaire sur le poste.

## Configuration du compte marchand

1. Créer et valider le compte marchand dans https://app.saspay.me, terminer le KYC et générer une clé **test**, scope `PAYIN`.
2. Vérifier les réseaux réellement disponibles au Togo et les tarifs du marchand dans le tableau de bord / `GET /pricing/my-rates/`. Les modes affichés par Missanye ne garantissent pas la disponibilité d’un opérateur chez SasPay ; le choix définitif se fait sur la page hébergée.
3. Configurer les frais en **ADD_ON** et activer l’autorisation de choisir le mode de frais par l’API (`ALLOW_CLIENT_OVERRIDE`). Le code vérifie les montants réels : il ne valide pas automatiquement un paiement dont les frais ont réduit le montant attendu.
4. Créer, dans le tableau de bord SasPay, un webhook **test** vers :
   `https://church-project-spring-pmbz.onrender.com/webhooks/saspay`
   Abonner `transaction.success`, `transaction.failed`, `transaction.cancelled`, éventuellement `transaction.created`. Copier le secret de signature lors de sa création ; il ne sera pas réaffiché.
5. Dans les variables du **backend Render**, définir :

| Variable | Valeur |
| --- | --- |
| `SASPAY_ENABLED` | `true` pour les essais configurés ; `false` désactive les appels |
| `SASPAY_API_KEY` | clé `sk_test_…`, stockée comme secret |
| `SASPAY_WEBHOOK_SECRET` | secret de signature du webhook test |
| `APP_URL` | `https://church-project-spring-frontend.onrender.com` |
| `SASPAY_COUNTRY` | `TG` |
| `SASPAY_API_BASE_URL` | `auto` (défaut) : sélection selon la clé ; URL explicite possible si elle correspond au même environnement |

Ne jamais copier une clé dans Git, un ticket, le chat, une variable `VITE_*` ou Desktop. Les variables de l’ancien prestataire ne sont plus lues. Sur un service Render existant, leur retrait du Blueprint ne les efface pas automatiquement.

## Vérification

Depuis un terminal disposant des variables, exécuter :

```sh
python3 Backend/scripts/check_saspay.py --offline
python3 Backend/scripts/check_saspay.py
```

La commande ne fait que des lectures et ne montre pas les secrets. Elle ne valide **pas** à elle seule le flux de paiement.

Tester ensuite une demande et un abonnement en sandbox : ouverture du checkout, retour navigateur, notification `webhook.test`, succès et échec. Vérifier le reçu, le crédit de la paroisse, l’activation de l’abonnement, et rejouer la notification : aucun second crédit ni seconde période ne doivent apparaître. Les paiements réels et ces essais sandbox avec un marchand ne sont **pas vérifiés tant que le compte n’est pas configuré**. Le passage en `sk_live_…` et le webhook live sont une étape humaine ultérieure.

## Comportement technique

- API revérifiée dans la spécification officielle le 21 novembre 2026 : https://docs.saspay.me/api-reference/openapi.json ; référence : https://docs.saspay.me/api-reference/payments et https://docs.saspay.me/api-reference/webhooks.
- `POST /checkout-sessions/`, montants sous forme de chaînes décimales et `ADD_ON`. Cet endpoint **ne prend pas en charge Idempotency-Key** ; il n’y a pas d’appel softpay ni de retrait automatique. Les demandes concurrentes sont sérialisées par le verrou facture / paroisse et réutilisent la session ouverte.
- Une réponse POST ambiguë est conservée en `UNCERTAIN`, sans répétition automatique de la création. Si l’ID distant manque, l’opérateur doit retrouver la session dans SasPay grâce à `metadata.missanye_reference` avant toute reprise. Ne pas supprimer cette réservation ou forcer une nouvelle session à l’aveugle.
- Le retour navigateur utilise un jeton signé et n’est jamais une preuve de paiement. Le backend relit `/checkout-sessions/{id}/status/` puis `/payments/{payment_id}/verify/` ; seul `SUCCESS`, avec le bon ID, la bonne devise, le montant attendu et le net attendu, autorise la validation.
- Les notifications sont vérifiées sur les octets bruts avec HMAC SHA-256 de `timestamp.body`, comparaison constante et fenêtre de 300 secondes. Accusé de réception après insertion durable et dédupliquée des IDs d’événements. Un worker dédié vérifie au maximum dix sessions par passage ; les notifications accélèrent ce contrôle. Le polling continue en secours et après redémarrage.
- La table `saspay_checkout` conserve session, transaction et montants exacts. La contrainte unique transaction et les verrous empêchent une double validation ; l’écriture comptable porte une référence `SASPAY:<transaction_uuid>`. Les montants canoniques décimaux sont aussi dans `details_paiement.provider_charged`, `provider_net`, `provider_fee`. Les anciens champs entiers restent des projections de compatibilité, les reçus exposent les valeurs décimales réelles.
- Les frais SasPay ne sont pas estimés avec un barème local : le site indique un sous-total et SasPay affiche les frais définitifs. La commission Missanye reste celle de `platform.*`. Un montant ou net incompatible passe en `REVIEW`, sans reçu payé, crédit ni activation automatique ; vérifier la configuration de frais et la transaction avant toute correction manuelle.
- Un refus `401/403` bloque la clé en mémoire jusqu’à changement de clé ou redémarrage ; `429` impose une pause d’une minute. Aucun POST financier n’est relancé automatiquement. Les corps d’erreur et secrets du prestataire ne sont pas exposés.

Pour surveiller les cas nécessitant une intervention :

```sql
SELECT id,purpose,target_id,state,remote_id,created_at
FROM saspay_checkout
WHERE state IN ('REVIEW','UNCERTAIN') ORDER BY created_at;
```

La checklist humaine détaillée est dans [SASPAY_TESTS.md](SASPAY_TESTS.md). Le diagnostic et les tests locaux ne remplacent pas cette recette.

## Prévenir les refus liés à l'environnement

Par défaut, `SASPAY_API_BASE_URL=auto` choisit le serveur sandbox pour une clé de
test et le serveur live pour une clé live. Les URL explicites restent possibles :
`sandbox-api.saspay.me/api/v1` pour la sandbox, `api.saspay.me/api/v1` pour le live,
avec HTTPS. Un couple clé/URL incompatible est refusé **avant tout appel**, sans
essai sur l'autre environnement. Les espaces extérieurs de la clé sont retirés ;
les guillemets et espaces internes sont refusés, sans afficher la valeur.

Les logs de configuration indiquent seulement le mode de clé, l'hôte API, les
indicateurs de présence et la validité de la configuration. Les erreurs distinguent
HTTP 401 (authentification) et HTTP 403 (droits, scope ou marchand). Aucun de ces
codes ne prouve à lui seul un KYC invalide. Le refus est mémorisé pour le couple
clé/URL : une correction effective de l'un des deux permet un nouvel appel explicite.
Sur Render, redéployer après modification des variables active la correction.
Aucun POST financier n'est répété automatiquement et aucun basculement d'hôte
n'est tenté après un échec.

Le script de diagnostic applique les mêmes règles de sélection d'environnement,
refuse les redirections HTTP et n'effectue que des GET. Une clé live nécessite
`--allow-live`. Sa réussite ne confirme ni le paiement ni le webhook réel.
