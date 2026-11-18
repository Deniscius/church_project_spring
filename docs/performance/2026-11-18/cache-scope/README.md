# Cache par paroisse — correctif et comparaison du 18 novembre 2026

## Changement livré

Les pages de demandes, statistiques et programmations ne sont plus vidées globalement lors d'une mutation de demande ou de créneau. Leur clé contient la paroisse, sa révision PostgreSQL validée, la révision des référentiels globaux et les paramètres de lecture. Le nom de la paroisse déjà résolue distingue aussi les instantanés lors d'un renommage concurrent.

- Une écriture validée dans A change l'espace de cache de A ; les entrées de B restent utilisables.
- Un rollback ne change pas la version visible aux autres transactions et ne supprime pas l'entrée précédente.
- Une lecture démarrée sur une ancienne version peut finir, mais ne remplit que son ancienne clé.
- Les transactions d'écriture contournent le cache : aucun DTO non validé ne peut être publié sous une révision qui serait ensuite réutilisée après rollback.
- Deux JVM avec des caches indépendants consultent les mêmes révisions PostgreSQL. Les autorisations sont toujours vérifiées avant le cache, y compris lorsque les entrées sont chaudes.
- Les changements des référentiels globaux restent volontairement globaux. Les caches des listes publiques globales ne sont pas modifiés.

Les suppressions, paiements, validations, changements de dates et de catalogue bénéficient des compteurs transactionnels déjà créés par les migrations V36/V37. Aucune nouvelle migration, dépendance Redis, modification du frontend ou mise à jour desktop n'est nécessaire.

Les TTL et tailles maximales restent identiques. Les anciennes clés deviennent inaccessibles aux lectures à jour et sont éliminées par expiration ou limite de taille. Le flux SSE n'a plus besoin de parcourir ou d'effacer les caches serveur avant d'envoyer une notification au web.

## Validation fonctionnelle

La suite backend comporte **104 tests réussis**, dont cinq tests PostgreSQL dédiés : isolation A/B pour les trois caches, rollback sans publication de données non validées, deux caches applicatifs indépendants, chargement ancien se terminant après un commit et renommage concurrent d'une paroisse. Les tests HTTP vérifient aussi le refus des pages et statistiques d'une autre paroisse après avoir réchauffé ses caches avec un compte autorisé.

## Comparaison de charge

Le même banc que l'[audit initial](../README.md) a été utilisé : dix paroisses, vingt agents, 20 000 demandes fictives, affinité Java sur un cœur logique, tas maximal 384 Mio, huit connexions Hikari et quarante threads Tomcat. La base est réinitialisée entre les versions. Le JAR de référence est copié avant compilation du correctif ; les signatures des JAR montés sont enregistrées dans [les métadonnées](environment.json).

Pour chaque version : préchauffage à 30 VU pendant 30 secondes, puis deux passages à 60 VU pendant 45 secondes. Chaque utilisateur attend une seconde après son parcours. La répartition reste 40 % horaires publics, 35 % pages + statistiques, 20 % révision/delta desktop et 5 % création fictive, sans paiement ni SMTP.

**16 095 requêtes sur les quatre passages mesurés, zéro erreur HTTP, 100 % des contrôles de statut, forme et paroisse réussis.** Les préchauffages sont exclus de ce total ; leurs seuils de latence peuvent être dépassés à froid.

| Passage | Cache pages : hits | Cache statistiques : hits | p95 pages | p95 statistiques | p95 création |
|---|---:|---:|---:|---:|---:|
| Avant 1 | 27,0 % | 41,5 % | 112 ms | 163 ms | 285 ms |
| Avant 2 | 25,1 % | 35,8 % | 82 ms | 141 ms | 241 ms |
| Après 1 | 75,3 % | 87,1 % | 74 ms | 96 ms | 263 ms |
| Après 2 | 74,3 % | 86,6 % | 63 ms | 77 ms | 173 ms |

Le résultat le plus net est la réutilisation des caches : environ **trois fois plus de hits pour les pages**, et des statistiques servies directement par le cache dans environ 87 % des cas. Les p95 des deux parcours ciblés baissent sur ces passages.

La requête coûteuse des premiers impayés est exécutée **516 fois avant** contre **118 fois après** sur le premier passage de chaque version, pour 4,05 s contre 0,83 s de temps SQL cumulé. La lecture des versions ajoute 1 828 requêtes indexées, totalisant environ 57 ms SQL dans ce passage local. Son temps réseau réel vers PostgreSQL doit être mesuré sur staging ; il n'est pas inclus dans le temps d'exécution SQL de `pg_stat_statements`.

## Limites et contreparties

- **Les p99 ne sont pas systématiquement meilleurs.** Pour les pages : 254/209 ms avant et 278/306 ms après. Le correctif améliore la réutilisation et les p95 observés, sans démontrer une amélioration de toutes les latences extrêmes.
- Le RSS maximal observé passe de 460–463 Mio avant à 462–477 Mio après. Ce correctif ne prétend pas réduire la mémoire ; les TTL et tailles maximales bornent toujours le cache. Il faut vérifier l'endurance et la marge sous un vrai quota mémoire.
- Docker rootless n'applique pas ses limites CPU/RAM dans cet environnement. L'affinité CPU et le tas Java sont explicites, mais l'hôte reste partagé. Les statistiques CPU/RSS proviennent du processus JVM, pas du cgroup parent.
- Deux passages par version sur un réseau local ne constituent ni une mesure de capacité Render ni un intervalle de confiance. Les petites différences de débit et de CPU ne sont pas une preuve de gain causal.
- Les programmations utilisent le même mécanisme et sont couvertes fonctionnellement ; ce scénario de charge mesure surtout les pages et statistiques. Il ne teste pas une bascule réelle entre deux instances.

## Suite recommandée

Conserver ce correctif, puis borner/paginer la liste d'impayés du tableau de bord et répéter la comparaison. Avant load balancing, restent les verrous des tâches planifiées, le stockage partagé des fichiers, l'anti-abus distribué et une validation sur staging. Le présent mécanisme règle la cohérence de ces trois caches entre JVM, sans rendre à lui seul tout le backend prêt pour plusieurs instances.

## Reproduction

Le banc accepte maintenant `PERF_JAR` pour monter en lecture seule un JAR de référence conservé hors des artefacts recompilés. Construire et conserver les deux versions, puis, pour chacune, préparer une base neuve et lancer :

```sh
# Avant : monter le JAR de référence lors de prepare et pendant les runs.
PERF_JAR=/chemin/absolu/reference.jar python3 performance/audit.py prepare --allow-shared-host
PERF_JAR=/chemin/absolu/reference.jar python3 performance/audit.py run --levels 30 --duration 30s --prefix cache_before_warm --allow-shared-host
PERF_JAR=/chemin/absolu/reference.jar python3 performance/audit.py run --levels 60 --duration 45s --prefix cache_before1 --allow-shared-host
PERF_JAR=/chemin/absolu/reference.jar python3 performance/audit.py run --levels 60 --duration 45s --prefix cache_before2 --allow-shared-host
python3 performance/audit.py down
# Après : construire le correctif, prepare, puis mêmes paliers avec cache_after*.
```

`--allow-shared-host` s'omet lorsque les quotas Docker sont effectivement appliqués. Les cibles restent contraintes au réseau Docker interne. Aucun service cloud payant n'est nécessaire.

[Résultats JSON](results.json) · [CSV](results.csv) · [Métadonnées et signatures](environment.json)
