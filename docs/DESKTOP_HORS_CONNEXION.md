# Demandes, validation et reçus sur le poste

## Activation et connexion

Lors de la première activation avec Internet, un utilisateur se connecte avec
son compte habituel et choisit une paroisse à laquelle il a accès. Un compte
local ne voit que ses paroisses autorisées ; les comptes globaux utilisent les
accès accordés par le backend. Le rattachement reste mémorisé pour les connexions
suivantes. Le développeur peut également fournir un profil préconfiguré dans
l'installateur. Les comptes, rôles et accès restent administrés sur le web.

Une première préparation en ligne est requise pour les identités et le catalogue.
Les ouvertures suivantes suivent le mécanisme Windows DPAPI décrit dans
[DESKTOP_PAROISSE.md](DESKTOP_PAROISSE.md). L'écran normal reste un formulaire
identifiant / mot de passe.

## Parcours métier sans Internet

1. Remplir la demande et choisir **Finaliser la demande**, ou finaliser un
   brouillon depuis Mon activité. Les champs, le forfait, son montant et le
   nombre de dates sont vérifiés à partir des informations disponibles sur le
   poste. L'écriture de la demande complète intervient après ces contrôles.
2. La demande apparaît immédiatement dans la liste, avec sa référence stable
   `MSL-<UUID>`. Elle est marquée comme enregistrée sur le poste.
3. Un forfait normal possède la validation automatique prévue par le modèle
   métier. Une demande spéciale attend une décision d'un utilisateur disposant
   de `demand:validate`. Une adresse e-mail est requise pour ce type de demande.
4. L'utilisateur disposant de `payment:manage` peut enregistrer les espèces
   après la validation. Cet enregistrement est durable et interdit un deuxième
   encaissement local de la même demande.
5. Avec `invoice:read` ou `receipt:manage`, le reçu PDF est généré sur le poste.
   Il précise la référence, le montant relevé, l'état de la décision, les espèces
   reçues et l'état de transmission. Sans encaissement, le document ne prétend
   pas attester le paiement. Les polices sont embarquées pour conserver un rendu
   lisible et les accents sur Windows.

Les décisions et espèces peuvent aussi être enregistrées pour une demande
serveur déjà chargée. Un encaissement dépendant d'une validation locale attend
la confirmation de cette validation avant son envoi. Les reçus et journaux
locaux restent chiffrés dans le coffre ; le PDF exporté est un document ordinaire.
Une réimpression du même état reprend les mêmes octets conservés, même après
redémarrage. Les versions antérieures des PDF restent dans le cache chiffré.

Le paiement par un prestataire en ligne nécessite Internet. Les places, tarifs,
délais et règles centrales sont revérifiés à la transmission ; le poste ne peut
pas réserver une disponibilité partagée pendant qu'il est isolé.

## Synchronisation et confirmation

La file transmet d'abord la création, ensuite la décision éventuelle, puis les
espèces. Chaque opération possède un UUID conservé après coupure ou reprise.
Les commandes dépendantes attendent une réponse valide de leur parent. Le
contenu envoyé est figé avant la première tentative, y compris l'identifiant et
la version serveur utilisés pour une mutation : une réponse perdue ne produit
pas une nouvelle demande, une nouvelle décision ou un second encaissement.

Pour une session ouverte grâce à l'identité préparée, le mot de passe saisi reste
seulement en mémoire le temps de rétablir une session auprès de l'API. Il est
ensuite effacé, ainsi qu'à la déconnexion ou fermeture. Le minuteur tente la
reconnexion avec temporisation ; **Actualiser** et **Transmettre les opérations**
peuvent également la déclencher. Si l'authentification ou les accès sont refusés,
une nouvelle connexion de l'utilisateur est nécessaire. Une saisie en cours
n'est pas réinitialisée lors de la reconnexion. Les attentes réseau et la
transmission s'exécutent en arrière-plan et laissent les formulaires disponibles.

Les permissions fraîches du serveur sont appliquées avant la reprise. Les
utilisateurs en consultation ne voient pas les actions d'écriture, de caisse
ou de transmission. L'API vérifie à nouveau chaque commande et ne fait pas
confiance à une validation ou un montant déclarés uniquement par le client.

Une réponse 403 bloque la commande ; un rejet métier la conserve comme à
rapprocher. Les commandes dépendantes ne passent pas avant une opération
rejetée ou bloquée. Les espèces réellement enregistrées ne sont ni annulées ni
considérées comme confirmées par ce rejet. Une demande liée à des espèces et
l'enregistrement de celles-ci ne peuvent être archivés ou remplacés pour faire
disparaître cette situation : elle exige un rapprochement par la paroisse.

## Code remis au fidèle

La référence locale reprend l'UUID de la création, indépendamment du poste qui
la produit. Le backend l'associe au code officiel dans la même transaction que
la demande et le résultat d'idempotence. Les recherches publiques de demande,
de facture et de reçu acceptent cette référence après transmission. Avant que
le service ait reçu la demande, le suivi n'existe que sur le poste concerné.
Le numéro affiché pour une demande locale demeure sa référence initiale.

Une ancienne réponse desktop sans alias est complétée lors d'une reprise après
mise à jour du backend. Le nouveau client refuse d'acquitter une création locale
si le service ne confirme pas l'alias attendu ; il conserve l'opération et ses
dépendances plutôt que de perdre le suivi du document remis au fidèle.

## Déploiement

**Mettre à jour le backend en premier**, avec la migration Flyway
`V32__desktop_tracking_reference.sql`, puis installer la nouvelle version desktop.
Les demandes et codes officiels existants restent accessibles.

Les tests couvrent la création locale, la décision, le reçu, le redémarrage,
les droits insuffisants, une réponse perdue, les dépendances, le rejet d'un
créneau et la reprise depuis une ancienne réponse. Le parcours réel avec Spring
Boot, PostgreSQL et HTTPS crée le document local avant transmission, synchronise
les espèces et vérifie ensuite le suivi et le reçu officiels via la référence
initiale. Les captures et un PDF de démonstration sont publiés dans l'artefact
`Missanye-Interface-Previews` du workflow Windows.

## Plusieurs postes

Ces changements concernent le travail autonome de chaque poste. Les demandes
non encore transmises ne deviennent pas visibles sur un autre ordinateur ou un
autre compte utilisateur isolé par simple génération d'un reçu. Le relais réseau
local évoqué pour partager ces opérations pendant une coupure Internet reste
à implémenter ; il devra conserver les mêmes UUID et les mêmes contrôles d'accès.
