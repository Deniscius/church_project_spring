# Correction de l'environnement et des refus SasPay — 21 novembre 2026

## Configuration Render appliquée

Les logs du backend `church_project_spring` montraient une clé détectée en sandbox
et un refus HTTP 401 sur une création de checkout. La spécification officielle
consultée le 21 novembre distingue bien deux hôtes :

- sandbox : `https://sandbox-api.saspay.me/api/v1` ;
- live : `https://api.saspay.me/api/v1`.

Une mise à jour MCP en mode fusion (`replace=false`) a fixé **uniquement**
`SASPAY_API_BASE_URL=https://sandbox-api.saspay.me/api/v1` sur le service backend
existant, dans l'espace Render déjà confirmé. Aucune clé ni aucun secret n'a été
lu, modifié ou affiché. L'opération a déclenché un déploiement du commit déjà en
production `c260983`, devenu actif ; `/actuator/health` a ensuite répondu HTTP 200
avec `status=UP`. Les logs confirment que la clé et le secret webhook sont présents
et que le mode détecté reste sandbox, sans afficher leurs valeurs.

Cette disponibilité ne prouve pas l'acceptation de la clé par SasPay. Aucun
checkout, paiement réel ou requête authentifiée vers SasPay n'a été initié par
l'agent. Les identifiants du backend ne sont pas accessibles au diagnostic local.

## Correctif de code préparé

Branche : `fix/saspay-environment-validation`, basée sur `c260983`.

- Valeur par défaut `SASPAY_API_BASE_URL=auto` : clé de test vers sandbox, clé
  live vers live. Une URL explicite reste autorisée si elle correspond à la clé.
- Un mélange clé/URL est refusé localement avant l'envoi ; aucun basculement
  automatique vers l'autre environnement après un échec.
- Espaces extérieurs des clés retirés, guillemets et espaces internes refusés.
- Hôtes HTTPS officiels, chemin `/api/v1`, port, absence d'identifiants et de
  query/fragment contrôlés ; HTTP localhost réservé aux tests simulés.
- Messages 401 et 403 distincts. Un 401 n'est pas présenté comme une preuve de
  problème KYC ; un 403 indique les vérifications de scope et de droits.
- Le refus mémorisé est lié au couple clé/URL capturé pour la requête. Une
  correction permet un nouvel appel explicite, sans rejouer l'appel précédent.
- Logs limités aux indicateurs de présence, au mode, à l'hôte autorisé et à la
  validité de la configuration ; aucune URL malformée ni corps d'erreur affiché.
- Diagnostic Python aligné sur ces règles, strictement GET, sans redirection ni
  répétition automatique. Une clé live requiert `--allow-live`.
- Exemples, Blueprints et documentation passent à `auto`. La variable Render
  explicite corrigée reste compatible ; un futur sync Blueprint utilisera la
  sélection automatique sans changer de mode pour la même clé.

Signature des webhooks, contrôles de succès et montants, espèces, reversements et
migrations restent inchangés. Aucun mécanisme de retry financier n'est ajouté.

## Vérifications exécutées

- **109 tests Java réussis**, zéro échec, erreur ou test ignoré, via le wrapper
  Maven Java 17 sur une base PostgreSQL 17 locale vide.
- **7 tests Python réussis** : sélection sandbox/live, rejet de mélange, opt-in
  live, clés mal copiées, URL interdites, absence de réseau et fuite en offline,
  erreurs HTTP distinguées sans répétition.
- Les tests Java existants de non-double crédit, non-double période, signature,
  frais incompatibles et ordonnanceurs ont été exécutés dans cette suite.
- `render.yaml` et `render-neon.yaml` valident contre le schéma JSON Render actuel.
- Aucun changement de migration ; `git diff --check` réussi.
- Diagnostic réel `--offline` : variables locales API key, webhook secret et
  APP_URL absentes, code de sortie 2. Cela ne décrit pas les variables Render.
- Aucun test sandbox marchand réel effectué ; aucune validation de KYC ou du
  scope réel revendiquée. Le frontend n'est pas modifié par ce correctif.

Commandes de reproduction avec l'environnement PostgreSQL de test défini dans
[SASPAY_RETIREMENT.md](SASPAY_RETIREMENT.md) :

```bash
(cd Backend && ./mvnw -q clean test)
python3 -m unittest discover -s Backend/scripts -p 'test_check_saspay.py'
```

Pour la vérification du véritable marchand, charger les variables secrètes en
privé selon [SASPAY_TESTS.md](SASPAY_TESTS.md), puis :

```bash
python3 Backend/scripts/check_saspay.py --offline
python3 Backend/scripts/check_saspay.py
```

Ne partager que le code HTTP et les messages expurgés, jamais une clé, une capture
de secret ou un corps contenant les données du client. La recette de paiement et
le rejeu du véritable webhook restent non vérifiés tant qu'ils ne sont pas faits.

## Fichiers concernés

- Client et configuration : `SasPayClient.java`, `SasPayProperties.java`,
  `application.properties`.
- Diagnostic et tests : `Backend/scripts/check_saspay.py`,
  `Backend/scripts/test_check_saspay.py`, `SasPayClientTest.java`.
- Exemples et CI : les deux Blueprints, les deux `.env` d'exemple du backend,
  `.github/workflows/ci.yml`, `.gitignore` (bytecode Python local).
- Documentation : `README.md`, `docs/SASPAY.md`, `docs/RENDER.md`,
  `docs/SASPAY_TESTS.md` et ce compte rendu.

Les nouveaux commits restent locaux jusqu'à l'accord de push demandé par
l'utilisateur. Le correctif de configuration Render est déjà actif ; les
protections de code décrites ici ne sont pas encore déployées.
