# Missanye Desktop 0.2.3

- Après le premier chargement, seules les demandes modifiées et supprimées sont récupérées lorsque le backend prend en charge la synchronisation incrémentale.
- Le catalogue est conservé tant que les paramètres, horaires et forfaits n'ont pas changé.
- Les données et le curseur sont enregistrés ensemble : une interruption de connexion conserve la dernière copie utilisable.
- Les demandes et opérations hors ligne gardent leurs identifiants, confirmations et conflits existants.

Le backend compatible est publié avec cette version. Les anciens backends restent utilisables avec le chargement complet. Les mécanismes existants de téléchargement et de proposition de mise à jour distribuent cet installateur après sa publication.
