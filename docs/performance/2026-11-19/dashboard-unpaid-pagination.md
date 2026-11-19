# Tableau de bord : demandes impayées paginées

## Problème et changement

Les statistiques chargeaient toutes les premières célébrations impayées dans la
fenêtre de rappel, puis filtraient leurs heures en Java et enrichissaient chaque
demande avec ses dates, sa facture et ses paiements. La réponse et le travail
d'enrichissement augmentaient avec le nombre d'impayés de la paroisse.

Le compteur `impayeesProchesCelebration` conserve le total correspondant à la
fenêtre configurée `app.demande.unpaid-reminder-days-before` (trois jours par
défaut). Le champ historique `impayeesProches` devient un **aperçu des cinq
demandes les plus urgentes**. Leurs DTO restent compatibles avec les clients
existants ; les cinq demandes récentes restent présentes.

La liste complète passe par :

```http
GET /demandes/paroisse/{publicId}/impayees-proches?page=0&size=10
```

L'endpoint utilise `PageResponse<UnpaidApproachingResponse>` : `content`, `page`,
`size`, `totalElements`, `totalPages`, `first`, `last`. La taille est bornée entre
1 et 100, la page à zéro minimum. Chaque ligne contient seulement l'UUID, le code,
le nom et le prénom, le libellé du type et `premiereCelebration`, sans historique
de paiement ni données de contact. Le web demande dix lignes par page.

PostgreSQL applique le filtre temporel, le `COUNT`, le tri et `LIMIT/OFFSET`.
L'heure respecte la priorité existante : heure personnalisée de la date,
horaire de la date, heure personnalisée de la demande, horaire de la demande,
puis minuit. Les bornes temporelles sont inclusives. Seule la date `ordre = 1`
est retenue ; les demandes et dates supprimées, les demandes payées et les statuts
hors `EN_ATTENTE` / `VALIDEE` sont exclus. Le tri est chronologique, puis par ID
pour stabiliser les égalités. Les index existants couvrent les filtres paroisse,
paiement et date ; aucune migration n'est ajoutée.

La permission `dashboard:view` et le contrôle d'accès à la paroisse précèdent
chaque lecture, y compris en cache. Le SQL JDBC porte explicitement le tenant et
les suppressions logiques, sans dépendre des filtres Hibernate. Le cache utilise
les révisions validées existantes et un TTL de vingt secondes. Le web invalide
ces pages avec les autres demandes sur les notifications de changement et
réinitialise la pagination en changeant de paroisse, sans conserver les anciennes
lignes pendant le chargement de la nouvelle paroisse.

## Vérification

- Quatre nouveaux tests PostgreSQL : 25 demandes réparties en pages 10/10/5,
  tri et égalités, total indépendant de l'aperçu, exclusions, autres paroisses,
  dates secondaires, bornes horaires et tous les replis d'heure, tailles extrêmes,
  page hors limites et liste vide.
- Le test HTTP authentifié d'isolation amorce aussi le cache de ce nouvel endpoint
  avant de vérifier le refus pour le secrétaire d'une autre paroisse.
- Essai Chromium du composant réel avec API fictive : navigation, première et
  dernière pages, changement de paroisse, page devenue vide, réessai après 503,
  affichage à 390 px sans débordement horizontal.
- Lint et compilation Vite.

Cette modification borne les lignes transférées et enrichies ; elle ne prouve pas
une capacité de production ni un gain de latence précis. Le comptage et le tri
restent proportionnels aux données candidates en base. Les pages sont relues
sur des données vivantes : une modification concurrente peut déplacer les lignes
entre deux pages. Le total et le contenu partagent le même prédicat mais pas un
instantané figé à travers plusieurs requêtes HTTP. Une mesure en environnement
isolé représentatif reste nécessaire avant le dimensionnement multi-instance.
