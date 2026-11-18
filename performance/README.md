# Audit de performance isolé

Ce banc n'utilise ni Render ni Neon de production. Il crée un projet Docker **missanye-perf**, PostgreSQL neuf et des données fictives : dix paroisses, vingt agents, 20 000 demandes, dates et factures. Aucun port n'est publié ; le réseau Docker est `internal: true`. Les paiements, SMTP et tâches métier périodiques sont désactivés. Les migrations restent celles du backend réel.

## Exécution

Prérequis : Docker Compose v2, Python 3, JDK 17 et Maven Wrapper.

```sh
./Backend/mvnw -f Backend/pom.xml -DskipTests package
python3 performance/audit.py prepare
python3 performance/audit.py run --levels 5 --duration 15s --prefix warmup
python3 performance/audit.py run --levels 5,15,30,60 --duration 45s --prefix baseline
# En dehors des paliers HTTP, pour ne pas fausser leur mesure :
docker compose -f performance/compose.yaml --profile load run --rm probe
python3 performance/audit.py down
```

Le générateur k6 accepte uniquement `http://app:8080`, avec un marqueur de données synthétiques. Il refuse une cible externe. Les niveaux sont bornés à 100 VU et la durée k6 à 120 secondes par palier. `down` supprime uniquement les conteneurs et volumes du projet Docker de test. Le seeding refuse toute base autre que `missanye_perf` et refuse d'écraser une fixture existante.

Les identifiants `PerfOnly-2026!` et JWT présents dans Compose sont **publics et réservés au banc local**. Ne jamais réutiliser ces comptes, secrets ou paramètres sur un service accessible au public. Les cookies obtenus au login restent en mémoire k6 ou dans un fichier temporaire du conteneur ; ils ne sont pas exportés dans les résultats.

## Ressources et validité

Compose demande 1 CPU / 512 Mio pour Java, 0,5 CPU / 512 Mio pour PostgreSQL, huit connexions Hikari et quarante threads Tomcat. La JVM est aussi liée explicitement à un CPU logique par `taskset`, avec un tas de 384 Mio et `ActiveProcessorCount=1`. `PERF_APP_CPU` choisit un numéro autorisé dans le conteneur ; `PERF_HEAP_MB` peut changer le tas (par exemple 256).

**Vérifier les contrôles effectifs.** Le script refuse par défaut Docker rootless sans cgroups délégués : les limites affichées par `docker inspect` ne prouvent pas leur application. `--allow-shared-host` autorise explicitement un test exploratoire dans ce cas, en enregistrant l'avertissement, les cgroups hérités et la commande Java. Les chiffres Docker Stats ne sont alors pas utilisés : ils peuvent décrire le cgroup parent partagé. Le suivi collecte la mémoire RSS/HWM et le temps CPU du processus JVM via `/proc/1`, ainsi que les métriques Hikari/JVM et les sessions PostgreSQL.

Un tas Java plafonné n'est pas une limite de mémoire totale : classes, piles, compilation JIT et buffers s'ajoutent au tas. La durée, la latence réseau locale, les ressources du générateur et le partage de l'hôte doivent être conservés dans toute interprétation. Ces tests ne dimensionnent pas à eux seuls l'offre Render et ne remplacent pas un test prolongé sur un staging aux quotas appliqués.

Exemple exploratoire si les quotas Docker sont indisponibles :

```sh
python3 performance/audit.py prepare --allow-shared-host
python3 performance/audit.py run --levels 5,15,30,60 --duration 45s --prefix heap384 --allow-shared-host
PERF_HEAP_MB=256 docker compose -f performance/compose.yaml up -d app
python3 performance/audit.py run --levels 5 --duration 15s --prefix warmup256 --allow-shared-host
python3 performance/audit.py run --levels 5,15,30,60 --duration 45s --prefix heap256 --allow-shared-host
```

## Modèle de charge et objectifs

Chaque utilisateur virtuel attend une seconde après son parcours. Les parcours sont distribués de façon déterministe : 40 % consultation publique des horaires, 35 % liste paginée + statistiques administratives, 20 % révision + delta desktop, 5 % dépôt de demande en espèces (sans encaissement). Les comptes sont répartis entre les dix paroisses. Les logins sont effectués une fois au démarrage de chaque palier ; leur durée ne figure pas dans les tendances par endpoint, mais compte dans le débit HTTP global k6.

Les contrôles vérifient les statuts, les formes des réponses et l'appartenance des demandes à la paroisse attendue. Les seuils exploratoires sont p95 < 500 ms en lecture, p95 < 1 000 ms en création et < 1 % d'erreurs HTTP. Ce sont des objectifs de travail, pas des SLA acquis. Le code de sortie k6 99 indique qu'un seuil a échoué et est conservé comme résultat ; un échec d'infrastructure arrête la série.

Le rate limiting par IP est désactivé **uniquement dans ce banc**, car tous les VU partagent l'IP du générateur. Pour le vérifier séparément, recréer l'app avec `PERF_RATE_LIMIT_ENABLED=true` et exécuter le probe de limitation :

```sh
PERF_RATE_LIMIT_ENABLED=true docker compose -f performance/compose.yaml up -d app
docker compose -f performance/compose.yaml --profile load run --rm probe python /work/rate_probe.py
```

Le probe SSE utilise vingt comptes et vérifie la connexion, les heartbeats et la notification des seuls clients de la paroisse modifiée. Les paliers HTTP n'incluent ni navigation navigateur, ni téléchargement initial complet du desktop, ni génération PDF, ni livraison SMTP, ni paiement fournisseur, ni bascule entre instances.

## Résultats

Les fichiers bruts, ignorés par Git, restent dans `performance/results/` : résumés k6, télémétrie JSONL, contrôles de ressources, métriques Prometheus et quinze requêtes SQL classées par temps cumulé (`pg_stat_statements`). Les sorties SQL peuvent contenir du SQL applicatif, mais cette fixture ne contient aucune donnée utilisateur réelle. Le rapport daté dans `docs/performance/` décrit les résultats sélectionnés, leurs limites et les priorités.

## Volume supplémentaire et plans SQL

Avant le probe anti-abus et avant l'arrêt, le volume peut être porté une seule fois de 20 000 à 100 000 demandes fictives :

```sh
docker compose -f performance/compose.yaml exec -T db psql -X -U perf -d missanye_perf -v ON_ERROR_STOP=1 < performance/scale-100k.sql
docker compose -f performance/compose.yaml exec -T db psql -X -U perf -d missanye_perf -At -v ON_ERROR_STOP=1 < performance/export-fixture.sql > performance/fixture.json
python3 performance/audit.py run --levels 60 --duration 45s --prefix volume100k
python3 performance/summarize.py
```

Ajouter `--allow-shared-host` seulement si nécessaire et assumé. `plans.sql` observe la pagination et le delta avec EXPLAIN ANALYZE ; `index-experiment.sql` évalue un index candidat dans une transaction qui est ensuite annulée. Ne pas exécuter ces observations pendant un palier de charge. Aucun index de production n'est modifié.

## Comparer deux versions du backend

`PERF_JAR=/chemin/absolu/reference.jar` permet de monter une copie figée du backend en lecture seule, sans l'écraser en recompilant le correctif. Le définir lors de `prepare` et des commandes de mesure. Le banc enregistre le SHA-256 du JAR réellement monté. Réinitialiser les données entre les deux versions, conserver les mêmes contrôles JVM et le même préchauffage. Voir la comparaison datée dans `docs/performance/2026-11-18/cache-scope/`.
