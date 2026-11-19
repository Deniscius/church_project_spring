# Répartition des traitements et synchronisation

Le backend reste un monolithe Spring Boot organisé en services métier. Le web et JavaFX n'autorisent jamais eux-mêmes une opération : le serveur revalide les droits, la paroisse, les versions et les montants. Les tables ajoutées sont dans PostgreSQL existant ; aucun abonnement Redis ou service supplémentaire n'est requis.

## Lectures et notifications

`GET /desktop/paroisses/{id}/revision` conserve son ETag et sa compatibilité avec les anciennes applications. Sa révision est maintenant calculée en temps constant à partir de compteurs transactionnels, au lieu de compter et agréger les lignes de nombreuses tables. Les mutations du catalogue, des droits, des demandes, dates, factures et paiements font évoluer ces compteurs dans leur transaction. Les changements globaux de comptes et de moyens de paiement invalident aussi les révisions. Un rollback ne produit pas de notification.

Le web ouvre une seule connexion SSE par espace administratif vers `/desktop/paroisses/{id}/events`. Les événements ne contiennent qu'une révision opaque. Ils invalident les requêtes de la paroisse active et rafraîchissent silencieusement le détail d'une demande, sans écraser une intention en cours de saisie. Le serveur contrôle les accès avant connexion et à chaque vérification. Le flux se renouvelle après 60 secondes via l'authentification normale ; une session expirée ne maintient pas une connexion indéfiniment.

Le serveur vérifie les révisions toutes les 15 secondes, envoie un heartbeat et limite les flux à 128 par instance, quatre par compte. Les flux sont fermés avant l'arrêt gracieux du serveur. Le navigateur ferme le flux lorsque l'onglet est masqué ou hors ligne ; en cas d'échec, il utilise une lecture de révision de secours à intervalle d'une minute et tente de reconnecter le flux après 30 secondes. Les autres pages publiques conservent leur comportement actuel.

Les caches de listes, statistiques et programmations utilisent des clés versionnées par les compteurs PostgreSQL de la paroisse et des référentiels globaux. Chaque lecture vérifie la version après les contrôles d'accès : une écriture validée change uniquement l'espace de cache concerné, y compris entre plusieurs JVM. Les écritures et les rollbacks ne publient pas de DTO non validés. Une ancienne lecture en cours ne peut remplir que son ancienne clé ; TTL et taille maximale éliminent ces anciennes versions. Le flux SSE signale le changement au web sans parcourir ni vider les caches serveur.

## Desktop 0.2.3 et 0.2.4

Les commandes hors ligne et leurs UUID stables sont conservés. Les conflits et rejets métier restent visibles et ne sont pas réessayés sous de nouveaux identifiants.

Deux endpoints, réservés à `demand:read` et contrôlés par paroisse, complètent le protocole :

- `/desktop/paroisses/{id}/sync/state` : curseur et révision du catalogue ;
- `/desktop/paroisses/{id}/sync/changes?after=N` : au plus 100 demandes modifiées, les UUID supprimés, le prochain curseur et `hasMore`.

Le premier chargement reste complet. Son curseur est lu **avant** le chargement pour que toute modification concurrente soit rejouée au passage suivant. Les suivants fusionnent les seules modifications, y compris les suppressions. Depuis 0.2.4, les lignes et leur curseur sont aussi lus dans un même instantané SQLite, y compris dans le coffre partagé entre comptes. Toutes les pages doivent réussir avant la sauvegarde atomique du résultat et du curseur dans le coffre local chiffré. Une interruption ou une réponse incohérente conserve la copie précédente.

Les compteurs sont sérialisés par paroisse jusqu'au commit : un changement ne peut pas être validé tardivement derrière un curseur déjà lu. Le journal conserve seulement la dernière révision par demande, ce qui borne sa taille au nombre de demandes modifiées, plutôt qu'au nombre d'éditions. Les changements de dates, de facture et de paiement rendent la demande concernée visible dans ce journal. Les suppressions physiques restent représentées par un UUID sans contenu.

Le catalogue n'est plus téléchargé à chaque changement d'une demande. Un changement du catalogue provoque un rechargement complet des projections de demandes, dont les libellés dépendent de ce catalogue. Un backend ancien (404/405 sur le nouvel endpoint) conserve le mode de chargement complet. Les versions desktop précédentes continuent de fonctionner avec le backend mis à jour.

## E-mails différés

`sendTextAsync` écrit maintenant dans `mail_outbox` dans la transaction métier. Les rappels de célébration et d'impayés utilisent aussi cette file. Les OTP d'inscription restent synchrones et conservent leur comportement de refus en cas d'échec SMTP.

Un ordonnanceur dédié prend au plus cinq messages par lot, avec verrouillage `SKIP LOCKED`, bail de dix minutes et numéro de tentative. L'appel SMTP se fait hors de toute transaction métier. Un échec programme une nouvelle tentative avec délai croissant (maximum une heure), jusqu'à huit tentatives. Un redémarrage permet de reprendre les baux expirés ; un ancien bail ne peut pas confirmer une nouvelle tentative. Les messages âgés de plus de 24 heures sont abandonnés. L'entretien horaire efface les traces terminées de plus de sept jours.

Destinataire, sujet et corps sont chiffrés en AES-GCM avec nonce aléatoire et identifiant du message comme donnée authentifiée. Le corps est effacé après livraison. La clé est dérivée de `MAIL_OUTBOX_KEY`, ou de `JWT_SECRET` si la première n'est pas définie. **Pour faire tourner JWT_SECRET sans perdre les messages en attente, définir une clé dédiée stable dès le déploiement initial de la file.** Après un changement de clé de chiffrement, les anciens messages non livrés ne seront plus déchiffrables ; préparer la rotation en vidant la file avant changement. Ne jamais enregistrer cette clé dans Git. Les erreurs conservées ne contiennent que le type d'exception.

SMTP peut accepter un message puis couper la connexion avant l'accusé de réception. La livraison est donc **au moins une fois** : un doublon d'e-mail reste possible après une panne. Cette file ne modifie jamais l'état financier d'une demande et ne relance pas de paiement.

## Exploitation

Les migrations Flyway V36 et V37 s'appliquent avec le backend avant diffusion du desktop. Le workflow Windows publie automatiquement la version 0.2.4 après fusion sur master ; le site et la recherche de mise à jour existants suivent la dernière release.

Réglages facultatifs : `PARISH_STREAM_DELAY_MS` (15000), `MAIL_OUTBOX_DELAY_MS` (10000), `MAIL_OUTBOX_KEY`. Conserver les timeouts SMTP bornés déjà configurés. Suivre les erreurs `E-mail différé non envoyé` et, via un accès administratif à PostgreSQL, les compteurs `SELECT state,count(*) FROM mail_outbox GROUP BY state`. La file ne prétend pas que l'e-mail a été livré au moment où l'opération métier est enregistrée.

Le verrou du compteur est pris par les écritures d'une même paroisse : éviter les transactions longues. Avant toute multiplication d'instances ou ajout de services, mesurer latence, pool JDBC, backlog d'e-mails, nombre de flux et durée des transactions. Le nombre de threads et connexions SSE est borné ; ce changement ne démontre pas à lui seul une capacité sous forte charge.
