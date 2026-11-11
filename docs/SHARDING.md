# Stockage partagé et préparation du sharding

## Ce que cette version met en place

La base PostgreSQL Neon reste partagée par toutes les paroisses. La migration
Flyway **V33** crée `parish_storage_placement`, indexé par l’UUID public de la
paroisse. Toutes les paroisses existantes sont affectées à `primary`. Une nouvelle
paroisse reçoit cette affectation dans la même transaction que sa création,
y compris lorsqu’elle est créée par un import SQL. Les paroisses système sont
également enregistrées. Une suppression logique conserve l’affectation.

Le registre est technique : aucune adresse de base ni aucun secret n’est transmis
au web, au desktop ou aux utilisateurs. Les autorisations RBAC et les contrôles
paroissiaux existants restent la source d’autorité. Une affectation ne donne aucun
accès aux données.

Avant de traiter une commande desktop, le backend vérifie les permissions et
l’accès à la paroisse, puis verrouille en lecture son affectation jusqu’à la fin
de la transaction. Une affectation absente ou non prise en charge produit **503**,
pas un rejet métier : le desktop conserve la commande pour une reprise avec son
UUID initial. L’historique d’idempotence vérifie aussi explicitement la paroisse,
en plus de l’auteur, du type et du contenu de la commande. Un index
`desktop_operation(parish_id, created_at)` prépare les extractions par paroisse.

**Ce registre n’est pas un routeur JDBC.** La contrainte SQL
`parish_storage_shared_only` interdit pour l’instant une affectation différente
de `primary`. Il n’existe aucune commande publique permettant de déplacer une
paroisse. Changer `SPRING_DATASOURCE_URL` change la base de toute l’application ;
ce n’est pas un déplacement de tenant.

## Pourquoi ne pas répartir immédiatement les bases

Les entités métier, utilisateurs, accès, catalogues et écritures comptables sont
actuellement liés par des clés étrangères dans une même base. L’authentification,
les parcours publics par code de suivi, les tâches planifiées et les vues globales
utilisent aussi cette base. Un simple `AbstractRoutingDataSource` choisi depuis
un en-tête ou le tenant courant ne traite pas ces dépendances et peut envoyer
une requête vers la mauvaise base.

La séparation actuelle repose sur les contrôles d’accès applicatifs et les
filtres existants. Cette migration n’ajoute ni RLS PostgreSQL, ni isolation
physique, ni partitionnement natif des tables.

## Architecture cible pour une répartition physique

- Un registre central conserve les UUID des paroisses, leurs affectations
  explicites, l’état d’une migration et une version d’affectation. Aucun calcul
  `hash(tenant) % nombreDeBases` : ajouter une base ne doit pas déplacer les
  paroisses existantes.
- Les comptes, appartenances et règles de la plateforme restent accessibles à
  l’authentification centrale. Il faudra remplacer les clés étrangères entre
  utilisateurs centraux et données réparties par des références UUID adaptées.
- Chaque paroisse conserve ensemble demandes, célébrations, catalogues,
  encaissements, factures, écritures rattachées, opérations desktop et alias de
  suivi. Les relations avec les abonnements, comptes et reversements de la
  plateforme doivent être inventoriées avant de décider de leur emplacement.
- La recherche publique par code nécessite un annuaire central permettant de
  retrouver la bonne base sans interroger arbitrairement tous les shards.
- Une transaction métier s’exécute dans une seule base de données. Les travaux
  entre plusieurs bases nécessiteront des événements durables et une reprise,
  plutôt qu’une transaction distribuée implicite.
- Les traitements planifiés et les rapports globaux doivent parcourir les
  affectations explicitement et éviter tout contexte de paroisse résiduel.

Le desktop continue à appeler la même API publique. Il ne choisit pas une base.
Ses UUID de commande et de demande restent inchangés, quelle que soit
l’affectation future. Il ne faut pas purger les opérations d’idempotence ou les
alias de suivi lors d’une migration.

## Conditions d’un futur déplacement

Avant d’autoriser une valeur autre que `primary`, implémenter et vérifier :

1. Deux bases réelles avec migrations Flyway et pools distincts, identité
   centrale et authentification/autorisation testées sur chaque parcours.
2. Un inventaire complet des tables, dépendances et fichiers de la paroisse ;
   les références aux pièces jointes et logos doivent rester valides.
3. Un état de maintenance de la paroisse, renvoyant une réponse réessayable à
   tous les parcours d’écriture : web, desktop, tâches et notifications de
   paiement. Un verrou limité aux commandes desktop ne suffit pas.
4. Une copie cohérente, incluant paiements, reçus, historique d’idempotence et
   alias ; contrôle des comptes, montants, relations et UUID avant bascule.
5. Une bascule atomique du registre, une invalidation contrôlée des caches de
   routage, et la garantie qu’un ancien serveur ne continue pas d’écrire dans
   l’ancienne base.
6. Un test de reprise d’une commande acceptée avant bascule dont la réponse a
   été perdue, et un test de lecture d’un ancien reçu par sa référence initiale.
7. Une procédure de retour tenant compte des nouvelles écritures : après reprise
   du trafic, rétablir simplement l’ancienne URL ne constitue pas un retour sûr.

Ces mécanismes de déplacement ne sont pas implémentés dans V33. Le registre les
prépare sans redistribuer les données existantes.

## Déploiement et contrôle

Déployer le backend normalement : Flyway applique V33 dans la base Neon actuelle.
Aucune nouvelle base, variable d’environnement ou dépense par paroisse n’est
nécessaire. Vérifications SQL en lecture seule :

```sql
SELECT shard_key, count(*) FROM parish_storage_placement GROUP BY shard_key;

SELECT p.public_id, p.nom
FROM paroisse p
LEFT JOIN parish_storage_placement s ON s.parish_id = p.public_id
WHERE s.parish_id IS NULL;
```

La première requête ne doit retourner que `primary`. La seconde ne doit retourner
aucune ligne. Une affectation manquante est une anomalie à diagnostiquer ; le
backend ne choisit jamais silencieusement une autre base.

Les tests PostgreSQL couvrent la migration, l’enregistrement des paroisses,
le refus d’un shard non pris en charge et la réponse 503 lorsqu’une affectation
manque. Les tests d’opérations desktop conservent leurs scénarios de concurrence,
de reprise, de caisse et de suivi.

## Plusieurs ordinateurs et coupure Internet

Le sharding ne partage pas les opérations entre deux ordinateurs privés d’Internet.
Chaque poste conserve son stockage protégé et sa file durable. Le relais LAN
facultatif reste un chantier distinct : authentification des postes, droits
paroissiaux, journal partagé, sauvegardes, reprise après arrêt du relais et
synchronisation vers le cloud devront être traités ensemble. Aucun nouveau
serveur dédié n’est imposé à la paroisse par V33.
