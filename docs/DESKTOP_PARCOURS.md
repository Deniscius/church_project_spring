# Vérification des parcours desktop

Périmètre : les parcours métier du desktop. La gestion des comptes et des rôles
reste dans les services existants ; elle n'est pas ajoutée à l'application.

## Comportements vérifiés

| Parcours | Comportement livré | Vérification |
|---|---|---|
| Première connexion | Un échec de récupération des paroisses ne crée pas de coffre incomplet utilisable hors ligne | Serveur HTTP simulé et interface JavaFX |
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

La suite desktop compte 32 tests exécutés et réussis, y compris le test contre
Spring Boot et PostgreSQL locaux sur une base jetable. Ce parcours crée une
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
