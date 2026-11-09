# Vérification des parcours desktop

Périmètre : les parcours métier du desktop. La gestion des comptes et des rôles
reste dans les services existants ; elle n'est pas ajoutée à l'application.

## Comportements vérifiés

| Parcours | Comportement livré | Vérification |
|---|---|---|
| Première connexion | Compte vérifié en ligne, sélection de paroisse, confirmation du mot de passe local et préparation complète avant ouverture ; un téléchargement interrompu n’active pas le hors-ligne | Serveur HTTP simulé et interface JavaFX |
| Test du serveur | Compteur pendant l’attente, panneau vert ou rouge et résultat visible ; enregistrement bloqué après échec | Contrôles HTTP et interface JavaFX redimensionnée |
| Dialogues | Taille adaptée à l’écran, défilement des contenus longs, boutons accessibles ; un créneau invalide garde la fenêtre ouverte | Test de 30 célébrations et affichage compact |
| Admin seed | Authentification initiale et session cookie vérifiées sur une base neuve ; absence de paroisse présentée après authentification comme un état de préparation | Seeder backend réel en HTTPS, refus de mauvais mot de passe et dialogue JavaFX |
| Accès local | Mot de passe local, propriétaire et environnement isolés ; les droits sont revérifiés en ligne | SQLite chiffré et contrats HTTP |
| Saisie incomplète | Un brouillon peut être enregistré sans téléphone ou avec une date à compléter ; il ne peut pas être transmis ainsi | Réouverture du coffre et action de sauvegarde réelle |
| Nouveau formulaire, changement de paroisse, déconnexion, fermeture | Choix enregistrer / abandonner / annuler avant de perdre une saisie ; pas de fermeture en plein traitement | Dialogues JavaFX et vérification du brouillon après déconnexion |
| Actualisation | Les champs, forfaits et sélections par identifiant restent stables ; une insertion ne déplace pas l'action vers une autre demande | Réponses HTTP changeantes et test de sélection |
| Transmission | Même identifiant après coupure ou redémarrage ; données validées avant mise en attente | Simulation de réponse incertaine et réouverture SQLite |
| Retour au brouillon | Possible uniquement pour une demande sans aucune tentative d'envoi ; impossible pour un encaissement | Vérification de l'état et du compteur de tentatives |
| Correction d'un rejet | Nouveau brouillon lié au rejet initial, conservé comme remplacé ; une seule correction par rejet | Régression de stockage |
| Refus de droits | Commande bloquée, autres commandes poursuivies ; reprise avec le même identifiant après rétablissement des droits | Contrats HTTP 403 et rejeu |
| Encaissement | Montant entier positif, pas de doublon local, pas d'encaissement pendant une mutation en attente | Tests de validation et de file |
| Réponse réseau incohérente | Une réponse HTTP 200 sans confirmation valide reste en attente ; montant, cible et statut sont vérifiés pour les espèces | Réponses simulées incomplètes ou incohérentes |
| Confirmation financière | Réponse et copie de la demande changent dans une seule transaction locale ; un échec annule les deux écritures | Erreur SQLite injectée puis reprise |
| Reçu | PDF officiel téléchargé, copie locale explicitement signalée en cas de coupure ; export sans tronquer le fichier précédent | Backend réel et tests de fichier |
| Plusieurs instances | Une seule application peut utiliser le même répertoire de données | Verrou système, libération et réouverture |
| Tableaux | Recherche et tri compatibles, montants triés numériquement, états lisibles | Interactions JavaFX |

## Validation réalisée

La suite desktop compte 45 tests exécutés et réussis, y compris le test contre
Spring Boot et PostgreSQL locaux sur des bases jetables, en HTTPS avec cookie Secure / SameSite=None. Ce parcours prépare réellement le coffre, crée une
demande, enregistre un encaissement de test, vérifie les conflits de correction,
supprime une autre demande et relit un PDF hors connexion.

Les tests JavaFX utilisent un affichage headless. Ils ne constituent pas une
validation manuelle de l'installateur Windows. La compilation et le cycle
installation/désinstallation sont confiés au workflow `Desktop Windows`.
Aucun paiement réel FedaPay n'est exécuté : ce parcours nécessite une recette
avec les paramètres sandbox du déploiement.

La protection de saisie couvre la fermeture normale de l'application. Une
coupure de courant ne peut préserver une saisie qui n'a jamais été enregistrée ;
les brouillons enregistrés et les opérations en attente restent sur disque.

## Première utilisation d'un poste

Une connexion Internet est obligatoire pour chaque nouveau compte sur un poste.
L'utilisateur se connecte avec son compte existant ; l'assistant propose les
paroisses actives auxquelles il a accès, sans configuration technique de l'API.
Il choisit la paroisse et confirme un mot de passe local d'au moins 12 caractères.
Ce secret protège les données de cet ordinateur et ne remplace pas le mot de
passe du compte. Il n'est pas récupérable par le serveur.

Les offres, horaires, moyens de paiement et toutes les pages de demandes sont
récupérés avant la création du nouveau coffre. La session, les données et le
choix de paroisse sont ensuite enregistrés ensemble dans une transaction
chiffrée. L'écran métier s'ouvre après cette préparation ; l'accès hors connexion
reste indisponible jusque-là. Une réponse vide valide est acceptée ; une réponse
incomplète ou une erreur réseau impose de réessayer en ligne.

Les connexions suivantes retrouvent la paroisse sélectionnée. Hors connexion,
seules les paroisses déjà préparées sont proposées. En ligne, le changement vers
une nouvelle paroisse déclenche sa préparation. Les coffres existants qui
contiennent déjà un catalogue et une liste de demandes complets restent
compatibles. Les nouveaux profils acceptent les variations de casse de
l'identifiant sans créer plusieurs coffres.

## Diagnostic des refus de connexion

Un refus HTTP 401 de `login-multi-tenant` indique que le serveur sélectionné
refuse l'identifiant ou le mot de passe. Si cet appel réussit mais que `auth/me`
refuse la session, le message précise que les identifiants ont été acceptés et
invite à vérifier les cookies et l'adresse du backend. Aucun secret, cookie ou
corps de requête n'est affiché. Le serveur effectif et le résultat sont visibles
près des champs de connexion.

Le test du vrai seeder (`LiveSeedAccountTest`) utilise une seconde base jetable,
initialement vide, avec `APP_ADMIN_SEED_USERNAME=desktop-seed-admin` et le mot de
passe de test `CiDesktopSeed-2026!`. Le lancer uniquement dans cet environnement
avec `DESKTOP_SEED_E2E=true` et `DESKTOP_SEED_API_URL=https://localhost:18444`.
Le serveur de validation utilise un certificat local explicitement approuvé par
le truststore de test ; la validation TLS n'est jamais désactivée.
Ces tests n'établissent pas la validité d'un compte ou de la configuration Render
réels : le refus constaté en production n'a pas été reproduit sur les bases de
validation.
