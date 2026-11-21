# Retrait FedaPay — compte rendu local du 21 novembre 2026

Branche : `refactor/saspay-only`, basée sur `bd4f0bd` (master après PR #106).
Les commits restent locaux : aucun push, PR, déploiement ou changement de variable
Render n'a été effectué pour cette intervention.

## Garde-fou de production

L'utilisateur a demandé à l'agent d'exécuter le comptage. La requête exacte :

```sql
SELECT count(*) FROM details_paiement
WHERE provider = 'FEDAPAY' AND statut_paiement = 'EN_ATTENTE';
```

Résultat : **0**, le 21 novembre 2026. Projet Neon **missanye**, branche
**production**, base `neondb`. L'hôte du compute a été comparé à celui enregistré
au démarrage Flyway du backend Render pour éviter de contrôler l'ancien projet
« Church Manage ». Aucun secret n'a été lu ou affiché et aucune donnée de
production n'a été modifiée. Le contrôle est ponctuel : le refaire avant une
future mise en production et examiner aussi les abonnements historiques en
attente, que cette requête ne compte pas.

La cartographie préalable a identifié les classes d'intégration, leurs tests,
les configurations, les Blueprints et la documentation. Les migrations et les
libellés historiques ont été distingués du code actif avant les suppressions.

## Changements

- Extraction de `CashPaymentSupport` : mêmes montants, frais nuls et libellés
  `MANUEL` / `CAISSE_LOCALE`, sans dépendance au prestataire. Comparaison avec
  l'ancien calcul exécutée avant sa suppression. Aucune modification du circuit
  de reversement manuel ni de la règle qui exclut les espèces du crédit en ligne.
- Retrait du client, du service, du vérificateur, des propriétés et du webhook
  historiques ; suppression du calculateur de frais exclusivement historique.
  `OnlinePaymentService` délègue désormais au seul prestataire actif.
- Colonnes et lignes historiques conservées ; montants enregistrés utilisés
  pour lire les paiements déjà réglés, sans recalcul selon la commission actuelle
  et sans appel au prestataire retiré. Libellés historiques des abonnements conservés.
- Hôte sandbox officiel ajouté à la liste HTTPS autorisée du client et du
  diagnostic, après lecture de l'OpenAPI actuelle. Les autres hôtes, identifiants
  intégrés aux URL et redirections non officielles restent refusés.
- Le checkout officiel ne supporte toujours pas `Idempotency-Key`. Aucune répétition
  automatique d'un POST financier n'a été ajoutée ; les réservations ambiguës
  restent en `UNCERTAIN`. La signature sur corps brut, HMAC SHA-256 de
  `timestamp.body`, la comparaison constante et la fenêtre de 300 secondes sont
  inchangées.
- Documentation et exemples de variables réécrits pour le checkout hébergé ;
  secrets `sync: false` dans les deux Blueprints. Les variables omises d'un
  Blueprint ne sont pas supprimées automatiquement des services Render existants.

## Vérifications réellement exécutées

| Vérification | Résultat |
| --- | --- |
| `mvn -q test` (commande globale) | Maven absent du PATH ; utilisation du wrapper Maven du dépôt avec Java 17 |
| Compilation Maven après retrait | réussie |
| Tests d'extraction espèces avant retrait de l'ancien calcul | 3 tests réussis, dont comparaison des snapshots et refus métier |
| Suite complète finale, PostgreSQL 17 local vide et données fictives | **106 tests, 0 échec, 0 erreur, 0 ignoré** |
| Démarrage via `ApplicationStartupSmokeTest` sans variables historiques | réussi ; aucun ancien bean ou mapping webhook enregistré |
| Nouveau test de règlement PostgreSQL | notification dédupliquée, un seul crédit messe, une seule échéance d'abonnement, REVIEW sans crédit, signature/expiration rejetées |
| Tests SasPay existants | client HTTP simulé, restrictions d'URL, absence de retry, signature, montants et ordonnanceurs exécutés |
| Lecture d'un paiement historique réglé | référence, charge, commission et net conservés, sans appel réseau au prestataire |
| `python3 Backend/scripts/check_saspay.py --offline` avec les variables réelles de cet environnement | code 2 : clé API, secret webhook et APP_URL absents |
| Diagnostic offline avec valeurs fictives, deux hôtes officiels et deux hôtes interdits | 4 cas réussis, aucune valeur secrète dans la sortie |
| Lint et build Vite | réussis |
| Schéma JSON Render actuel, `render.yaml` et `render-neon.yaml` | 0 erreur de schéma ; secrets en sync:false vérifiés |
| Diff des migrations Flyway existantes | vide |
| Paiement sandbox réel, tarifs marchand, notification réellement émise par SasPay | **non vérifiés** : pas de clé ni de secret marchand disponibles |
| Paiement réel / passage en live / validation manuelle d'un reçu historique de production | **non effectués** |

Un passage complet intermédiaire a échoué sur un comptage d'un test existant,
car la base locale contenait le dépôt créé par un passage antérieur. La suite
finale a été relancée sur un nouveau conteneur PostgreSQL vide et réussit. Ce
reliquat de données n'a pas été corrigé en modifiant les assertions métier.

## Références résiduelles volontaires

- Migration V7 inchangée : valeurs historiques du fournisseur et source d'activation.
- Sources d'activation et libellés comptables d'anciens abonnements.
- Protection de caisse reconnaissant une ancienne tentative en ligne avant de
  permettre un encaissement local.
- Tests de lecture historique et assertions que l'ancienne configuration/route
  n'existent plus ; documents retraçant le garde-fou.

Aucune propriété runtime, variable d'exemple ni route active de l'ancien
prestataire n'est conservée. Les anciens montants ne sont ni effacés ni réécrits.

## Commandes de reproduction

Avec un JDK 17, Docker et un terminal à la racine du dépôt :

```bash
docker run --name missanye-saspay-check -d \
  -e POSTGRES_DB=church_project_ci -e POSTGRES_USER=church_ci \
  -e POSTGRES_PASSWORD=church_ci_password \
  -p 127.0.0.1:55432:5432 postgres:17-alpine
# Attendre que pg_isready réussisse :
docker exec missanye-saspay-check pg_isready -U church_ci -d church_project_ci

export CI_POSTGRES_SMOKE=true SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/church_project_ci
export SPRING_DATASOURCE_USERNAME=church_ci SPRING_DATASOURCE_PASSWORD=church_ci_password
# Valeur publique fictive, exclusivement pour cette base de test locale :
export JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
export APP_ADMIN_SEED_ENABLED=false
(cd Backend && ./mvnw -q test)
(cd Frontend/church-frontend && npm ci && npm run lint && npm run build)
docker rm -f missanye-saspay-check
```

Recréer le conteneur vide pour chaque passage complet, le test HTTP de création
persistant volontairement ses fixtures. Ne pas connecter cette suite à la base
réelle. Les valeurs ci-dessus sont fictives et locales.

Pour les vrais essais sandbox, charger les secrets du marchand en privé puis :

```bash
python3 Backend/scripts/check_saspay.py --offline
python3 Backend/scripts/check_saspay.py
```

La procédure de chargement sans affichage des secrets, les réglages de l'API et
la recette demande + abonnement sont dans [SASPAY_TESTS.md](SASPAY_TESTS.md).
Chaque ligne de cette recette reste « non vérifié » jusqu'à son exécution humaine.

## Décisions et risques restants

- Autorisation explicite nécessaire avant le push, conformément à la demande.
- Recette sandbox obligatoire avant toute décision live. Le diagnostic ne prouve
  ni un encaissement ni la livraison du webhook réel.
- Refaire le comptage avant la bascule ; si un ancien paiement en attente est
  apparu, conserver l'ancienne version et le rapprocher avant retrait. Ne pas
  modifier arbitrairement un statut en base pour forcer le passage.
- Sur un service Render existant, saisir les variables SasPay dans Environment,
  vérifier le déploiement puis retirer manuellement les anciennes variables.
  Aucun de ces changements d'infrastructure n'a été effectué ici.
- `UNCERTAIN` et `REVIEW` exigent un rapprochement documenté, jamais un nouveau
  POST aveugle. Aucun endpoint de retrait automatique n'est ajouté.

## Fichiers créés, modifiés et supprimés

La liste ci-dessous couvre toute la branche par rapport au master de départ.
Les fichiers de logs et les données de test locales ne sont pas committés.

### Créés

- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/CashPaymentSupport.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/SasPaySettlementIntegrationTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/CashPaymentSupportTest.java`
- `docs/SASPAY_RETIREMENT.md`
- `docs/SASPAY_TESTS.md`

### Modifiés

- `.github/workflows/ci.yml`
- `Backend/.env.example`
- `Backend/.env.production.example`
- `Backend/scripts/check_saspay.py`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/ChurchProjectSpringApplication.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/DTO/response/PaymentFeeBreakdown.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/config/RateLimitProperties.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/entities/DetailsPaiement.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/entities/ParoisseAbonnement.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/security/PublicRateLimitFilter.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/security/SecurityConfig.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/billing/SubscriptionBillingService.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/impl/DemandeServiceImpl.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/impl/DetailsPaiementServiceImpl.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/OnlinePaymentService.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/PaymentReturnTokenService.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/saspay/SasPayClient.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/saspay/SasPayPaymentService.java`
- `Backend/src/main/resources/application-dev.properties`
- `Backend/src/main/resources/application-prod.properties`
- `Backend/src/main/resources/application.properties`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/ApplicationStartupSmokeTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/DashboardUnpaidIntegrationTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/cache/ParishResponseCacheIntegrationTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/impl/DetailsPaiementServiceImplTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/saspay/SasPayClientTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/saspay/SasPayPaymentServiceTest.java`
- `Desktop/README.md`
- `Frontend/church-frontend/.env.example`
- `README.md`
- `docs/DESKTOP.md`
- `docs/DESKTOP_PARCOURS.md`
- `docs/RENDER.md`
- `docs/SASPAY.md`
- `docs/SCALING.md`
- `performance/compose.yaml`
- `render-neon.yaml`
- `render.yaml`

### Supprimés

- `Backend/src/main/java/com/eyram/dev/church_project_spring/config/FedaPayProperties.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/controller/WebhookController.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/FedaPayPaymentService.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/PaymentFeeCalculator.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/fedapay/FedaPayClient.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/fedapay/FedaPayPhone.java`
- `Backend/src/main/java/com/eyram/dev/church_project_spring/service/payment/fedapay/FedaPayWebhookVerifier.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/FedaPayPaymentServiceTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/PaymentFeeCalculatorTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/fedapay/FedaPayPhoneTest.java`
- `Backend/src/test/java/com/eyram/dev/church_project_spring/service/payment/fedapay/FedaPayWebhookVerifierTest.java`
- `docs/FEDAPAY.md`
