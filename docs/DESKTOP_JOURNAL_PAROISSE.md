# Journal local partagé par paroisse et droits utilisateur

## Règle de propriété

Les brouillons, demandes finalisées, décisions, espèces et reçus appartiennent à
la **paroisse configurée sur le poste**. Ils ne sont plus des dossiers privés par
identifiant. Deux utilisateurs de cette paroisse sur le même poste et le même
profil Windows retrouvent ce journal, même sans Internet, après préparation de
leur propre connexion.

L’identité, le vérificateur de mot de passe, la session et les accès restent
propres à chaque compte. Le journal métier et le catalogue/données paroissiales
sont partagés, avec une clé aléatoire protégée par Windows DPAPI et un chiffrement
AES-GCM. Le serveur et l’UUID de paroisse définissent le périmètre de stockage.
Un autre tenant ou un autre serveur utilise un journal distinct.

Le rôle décrit les responsabilités ; les **permissions effectives** déterminent
les actions. Le libellé de rôle n’est jamais utilisé comme une clé de chiffrement
ou comme une autorisation implicite.

| Action | Permission requise |
| --- | --- |
| Voir l’espace de la paroisse et les demandes | `demand:read` + accès à cette paroisse |
| Créer, reprendre ou finaliser un brouillon | `demand:edit` |
| Valider / rejeter | `demand:validate` |
| Encaisser des espèces | `payment:manage` |
| Exporter un reçu | `invoice:read` ou `receipt:manage` |
| Supprimer une demande serveur | `demand:delete` |

Exemple : un secrétaire prépare une demande ; un responsable habilité la valide ;
un caissier reçoit les espèces. Chacun voit l’état courant sur ce poste et agit
avec ses propres droits. Un compte en consultation peut voir le dossier, sans
le modifier ni produire un reçu s’il ne possède pas la permission correspondante.
Les contrôles sont appliqués dans le stockage partagé et les services, en plus
des boutons de l’interface et des vérifications du backend.

Le journal conserve `createdBy`, `createdByName`, `createdByRole` et `updatedBy`.
L’auteur initial est conservé lorsqu’un autre utilisateur reprend un brouillon.
Le rôle enregistré décrit l’auteur à la saisie (il reste vide pour les anciens
enregistrements dont ce rôle historique est inconnu) ; il ne donne pas de droit au
compte qui lit ou transmet ensuite. Le backend attribue la transmission au compte
réellement authentifié ; cette identité ne doit pas être confondue avec l’auteur
initial déclaré dans le journal local.

## Synchronisation après changement d’utilisateur

L’idempotence serveur est désormais définie par **paroisse + UUID d’opération**,
avec la migration V34. Une commande acceptée sous un compte, puis renvoyée sous
un autre compte après perte de la réponse, retourne la confirmation existante.
Elle ne recrée ni demande ni paiement. Le contenu, le type, les accès à la paroisse
et les permissions sont revérifiés avant de retourner ce résultat.

Le desktop transmet uniquement les commandes autorisées pour le compte connecté.
Une commande dont il ne possède pas la permission reste en attente, sans devenir
« bloquée » uniquement à cause de ce changement d’utilisateur. Ses dépendances
attendent aussi. Un utilisateur disposant de toutes les permissions nécessaires
peut transmettre toute la chaîne ; sinon les utilisateurs habilités transmettent
leurs catégories de commandes lors de leurs connexions successives. Les droits
enregistrés dans une opération hors connexion ne permettent pas d’usurper un
compte ni d’obtenir une permission serveur.

Avant de transmettre un journal partagé, le desktop exige
`GET /desktop/capabilities` avec `parishJournalVersion >= 1`. Un ancien serveur ne
reçoit pas ces commandes : cela évite qu’il applique encore une idempotence par
compte et traite deux fois un envoi après changement d’utilisateur.

## Mise à jour et anciens brouillons

1. Vérifier l’absence d’anciens UUID dupliqués dans une même paroisse :

   ```sql
   SELECT parish_id, operation_id, count(*)
   FROM desktop_operation
   GROUP BY parish_id, operation_id
   HAVING count(*) > 1;
   ```

   Si cette requête retourne des lignes, les auditer avant V34. La migration
   échoue plutôt que supprimer arbitrairement des justificatifs d’opérations.
2. Déployer le backend et laisser Flyway appliquer **V34** avant le nouveau client.
3. Avant la reprise des encaissements, transmettre les anciennes files avec
   l’ancienne version, ou organiser une ouverture de chacun des anciens comptes
   avec le nouveau client pour importer toutes les opérations locales. Un ancien
   encaissement encore privé ne peut pas être découvert par le seul compte d’un
   collègue.
4. Installer le nouveau desktop. Chaque compte conserve sa préparation d’identité
   et les règles de première connexion/validité déjà documentées.
5. À l’ouverture de chaque ancien compte autorisé, ses opérations de la paroisse
   sont importées dans le journal partagé avec leurs UUID, états, dépendances et
   corps de tentative figés. Les copies privées chiffrées restent en sauvegarde.
6. Un enregistrement déjà présent dans le journal partagé n’est jamais remplacé
   par une ancienne copie privée : le retour de l’auteur initial ne doit pas
   effacer la correction d’un collègue. Un ancien compte doit s’ouvrir au moins
   une fois pour importer ses anciennes opérations ; les autres comptes ne
   déchiffrent pas automatiquement tous ses anciens coffres.

Ne pas supprimer les fichiers `identities/parish-*.dpapi` ni partager le fichier
SQLite ouvert sur un lecteur réseau. Ne pas revenir à un ancien client pour
modifier les copies privées historiques : elles ne reflètent plus le journal
commun courant.

## Limites du partage

Ce partage est immédiat entre comptes de l’application sur ce poste et ce profil
Windows. Il ne partage pas automatiquement les données entre profils Windows
distincts ou entre ordinateurs privés d’Internet. Le relais LAN demeure un
chantier distinct. Les fenêtres et les reçus gardent le parcours métier normal,
sans choix de mode réseau demandé à l’utilisateur.

Les tests couvrent les rôles complémentaires, le refus d’une écriture par un
compte en consultation, l’isolation entre tenants, l’import sans écrasement,
le refus d’un ancien protocole et le rejeu réel entre deux comptes authentifiés.
