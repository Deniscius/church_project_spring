# Revue du parcours web et adaptation au Desktop

Périmètre : espace paroissial du web sur `master`, comparé aux écrans JavaFX,
aux services frontend et aux DTO/contrôleurs du backend. Le client Windows reste
rattaché à une paroisse ; les fonctions de plateforme multi-paroisses ne sont
pas intégrées dans ce parcours.

## Navigation et correspondances

| Parcours web | Accès Desktop | Champs et comportements repris |
| --- | --- | --- |
| Connexion / mot de passe oublié | Connexion et Mot de passe oublié | Compte web, serveur configuré, demande de lien par e-mail, réponse générique et reprise locale existante |
| Tableau de bord / programme quotidien | Calendrier paroissial → Célébrations du jour | Date, heure, fidèle, intention, paiement, confirmation de célébration, déplacement individuel selon permission |
| Nouvelle demande / récapitulatif | Nouvelle demande | Fidèle, téléphone, e-mail, coursier, intention, type, forfait, montant confirmé, dates multiples, horaires par date, heures personnalisées et délai minimum |
| Liste / détail des demandes | Demandes et paiements | Recherche par téléphone/code/nom/intention, filtres état et paiement, date de dépôt, validation ou rejet, correction de l’intention, encaissement, paiement en ligne, rapprochement, suppression selon état et droits |
| Édition complète d’une demande | Même limite que le web | La route web `EditRequestPage` redirige vers le détail : elle n’expose pas d’édition complète. Le Desktop conserve les brouillons et la correction d’intention, sans créer de doublon pour simuler une modification |
| Demandes archivées | Paramètres → Demandes archivées | Liste séparée réservée à la permission d’audit, date et auteur d’archivage |
| Programme des horaires | Calendrier paroissial → Programme du jour | Date, 1 à 20 créneaux, heure, libellé, nature de l’honoraire, remplacement de la journée et retour à la grille hebdomadaire |
| Horaires | Paramètres → Horaires des célébrations | Jour hebdomadaire ou date ponctuelle, heure, libellé, messe unique, honoraire imposé pour une date, activation, création/modification/suppression |
| Types de demandes | Types et forfaits → Types de demandes | Libellé, description, catégorie Eucharistie/Sacrement/Sacramentaux, délai 0–8760 heures, jours autorisés, activation, création/modification/suppression |
| Forfaits / honoraires | Types et forfaits → Forfaits et honoraires | Type de demande, code, nom, nature normale/dominicale/spéciale, montant XOF, durée, nombre de célébrations, jours restreints à ceux du type, heure personnalisée, description, activation |
| Équipe | Équipe et chorales → Équipe paroissiale | Identité et mot de passe initial à la création, rôle applicatif, fonction paroissiale, activation, rattachement à la paroisse courante ; modification du rôle/activation sans réinitialiser l’identité du titulaire |
| Chorales | Équipe et chorales → Chorales | Nom, description, modification/désactivation, invitation avec copie du jeton et date d’expiration |
| Reçu et identité paroissiale | Paramètres → Coordonnées, banque et reçu | Nom/adresse et e-mail professionnel consultables, téléphone, banque avec choix et nom libre, titulaire, RIB/IBAN normalisé et vérifié, logo PNG/JPEG, retrait du logo, export du reçu modèle |
| Feuille d’intentions | Demandes → Feuille d’intentions du jour | Date, sélection des messes, inclusion des intentions validées non payées, moteur PDF partagé avec le backend |
| Factures / détail | Trésorerie et factures → Factures | Référence, fidèle, intention, date, montant, statut, mode et détail du règlement (frais, net, transaction) |
| Paiements | Trésorerie et factures → Paiements | Demandes de la paroisse, coordonnées du fidèle, montant, statut et mode de paiement ; encaissement et rapprochement restent dans le parcours Demandes |
| Journal de caisse | Trésorerie et factures → Journal de caisse | Totaux, jour/mois, nombre d’encaissements, date, encaisseur, code, fidèle, montant et recherche |
| Compte paroissial | Trésorerie et factures → Compte de trésorerie | Solde disponible et en attente |
| Reversements | Trésorerie et factures → Reversements | Historique, montant, statut, motif et référence ; demande avec minimum 100 XOF, solde et RIB vérifiés côté serveur |
| Mon profil | Mon profil | Nom, prénoms et téléphone ; e-mail professionnel, identifiant et rôle non modifiables ; changement de mot de passe courant/nouveau/confirmation |
| Mise à jour de l’application | Paramètres → Application et mises à jour | Actualisation des données et parcours de mise à jour Windows existant |

## Points corrigés pendant la revue

- Les créneaux d’une journée personnalisée remplacent la grille hebdomadaire et
  les ajouts ponctuels, comme dans `HoraireServiceImpl.buildProgrammeDay`.
- Le service web du profil transmet maintenant le téléphone modifiable et
  préserve l’e-mail existant en lecture seule, au lieu de les omettre.
- Les référentiels sont réactualisés après fermeture des paramètres modifiés,
  sans effacer la demande en cours de saisie.
- Les paramètres disposent d’une protection des modifications non enregistrées
  lors du changement de rubrique, de sélection ou de la fermeture.

## Connexion, données locales et permissions

Les paramètres sont consultables depuis leur copie locale après un premier
chargement autorisé. Cette copie est séparée par compte, paroisse et rubrique,
et reste dans le stockage chiffré existant. Les rubriques sans droit ne sont pas
proposées. Les mots de passe et invitations ne sont pas conservés dans ce cache.

Les écritures de configuration nécessitent une connexion et une confirmation
serveur ; elles ne rejoignent pas la file des demandes hors connexion. Le client
revérifie la session et les permissions avant ces écritures. Les contrôles du
backend restent définitifs. Un changement de mot de passe invalide la reprise
locale préparée, et demande une nouvelle connexion.

## Vérifications

- Tests de calendrier pour la priorité d’une journée personnalisée.
- Tests HTTP réels du client pour PUT/PATCH/DELETE, réponse vide et changement
  de session ou de permission avant écriture.
- Tests des règles de rôle d’équipe, du cache par compte et des RIB/IBAN.
- Test JavaFX : création d’un horaire et présence de tous les paramètres du type
  de demande, avec capture de l’écran natif.
- Suite Desktop complète et CI Windows (construction, installation, mise à
  niveau conservant le rattachement paroissial, désinstallation).
