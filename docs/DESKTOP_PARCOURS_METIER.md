# Parcours métier, impression et paramètres du desktop

## Calendrier et forfait

Le desktop utilise les règles du calendrier web et de `DemandeSchedulingPolicy` :

- Les horaires sont filtrés par jour de semaine, dates spécifiques publiées,
  activité, nature de l’honoraire et délai minimum du type de demande.
- Une messe unique publiée à une date prend priorité sur les horaires hebdomadaires.
- Le changement de date sélectionne automatiquement un horaire compatible. Pour
  une demande simple, le forfait dominical ou l’honoraire publié est sélectionné
  lorsqu’un forfait correspondant existe ; le montant est affiché avant confirmation.
- Les séries génèrent le nombre de dates du forfait en respectant les jours
  autorisés et sa période. Une trentaine utilise 30 jours calendaires consécutifs.
  Chaque date reçoit son propre horaire du programme, modifiable dans le dialogue
  des créneaux. Une date sans horaire compatible doit être corrigée avant finalisation.
- Une heure personnalisée est proposée uniquement pour une demande/forfait spécial
  dont `heurePersonnalise` est activé. Une messe unique ne peut pas être contournée
  par une heure libre. Revenir à un forfait ordinaire efface cette heure.
- Les mêmes contrôles sont appliqués lors de la finalisation locale, pas seulement
  dans les champs de l’interface. Le backend vérifie encore les règles et les
  disponibilités à la transmission.

Les jours autorisés incompatibles entre type et forfait sont une erreur de
configuration, conformément au backend. Le montant et les disponibilités ne sont
pas réservés par une saisie locale. La confirmation reprend le montant présenté ;
si le tarif change pendant cette confirmation, l’utilisateur doit vérifier et
confirmer le nouveau montant. Les demandes déjà finalisées conservent leur devis
et leur preuve d’espèces éventuelle pour le rapprochement.

Les erreurs de saisie sont affichées près des actions du formulaire ainsi que dans
la barre d’état, avec une présentation distincte. Les dialogues gardent leurs
propres messages et restent ouverts tant qu’une valeur est invalide.

## Recherche, identité du fidèle et copie

La recherche des demandes accepte le téléphone international ou une partie du
numéro national, y compris les espaces, tirets et parenthèses usuels. Elle porte
aussi sur le nom, le code et l’intention. Les téléphones saisis sont normalisés
pour retirer ces séparateurs avant la validation du format international.

Sans nom ni prénom, l’affichage reprend la règle du web : **Un(e) chrétien(ne)**.
Aucun faux nom n’est injecté dans les données du fidèle.

Le bouton **Copier** et le menu contextuel de la liste proposent :

- Code de suivi ;
- Téléphone du fidèle ;
- Détails de la demande, en texte lisible.

Le raccourci Ctrl+C dans la table copie le code de la ligne sélectionnée. Les
champs de texte conservent leurs raccourcis de copie habituels.

## Reçu et fiche d’intention

Le code de suivi du reçu occupe une ligne entière en caractères gras de 12 points.
Son UUID n’est pas raccourci : la référence remise avant transmission demeure
utilisable après synchronisation. Les anciennes références restent acceptées.
Le PDF garde ORIGINAL et DUPLICATA sur une seule feuille A4.

**Feuille d’intentions du jour** reprend le document du web : date de célébration,
sélection d’une ou plusieurs messes et regroupement des intentions par heure.
Par défaut, seules les demandes validées et payées sont retenues ; une option
permet d’inclure les demandes validées non payées. La progression des séries
utilise les célébrations réellement enregistrées, sans déduire leur exécution
du seul passage de la date. Le fidèle non renseigné devient « Un(e) chrétien(ne) ».
Le PDF s’ouvre dans le lecteur habituel pour impression. Il utilise les données
présentes sur le poste et exige l’accès de consultation à la paroisse.

Le web et le desktop utilisent le même moteur de mise en page et les mêmes
polices embarquées. Le backend doit être redéployé pour fournir les indicateurs
de célébration de chaque date ; reconstruire ensuite l’installateur desktop.

Les fichiers exportés sont des PDF ordinaires. Les captures et exemples de PDF
se trouvent dans l’artefact CI `Missanye-Interface-Previews`.

## Paramètres et modifications faites sur le web

Le menu **Paramètres** présente la paroisse, le compte, la version et les actions
usuelles : actualiser les données et rechercher une mise à jour. L’adresse de
l’API et les paramètres de base de données ne font pas partie de ce parcours.

Lorsque la session est disponible, les demandes, horaires, forfaits et autres
référentiels sont rechargés toutes les minutes, y compris pour les comptes en
consultation. Les textes en cours de saisie sont conservés. Un horaire ou un
forfait devenu indisponible doit être sélectionné à nouveau ; une intention
complète n’est pas effacée par l’actualisation. Les permissions fraîches du serveur
continuent à gouverner les actions visibles et les commandes transmises.

## Nouvelles versions

Le desktop lit les releases publiques du dépôt GitHub, sans jeton ni cookie de
session. Seules les releases stables nommées `desktop-vX.Y.Z` possédant un
installateur non vide `Missanye-Setup.exe` sont proposées. Une release de la partie
web, un brouillon, une préversion ou un lien hors du dépôt est ignoré.

La vérification automatique a lieu au lancement, à la reconnexion et au plus
une fois par heure en session. Une recherche manuelle reste disponible dans
Paramètres. **Installer la mise à jour** télécharge l’installateur et son empreinte
SHA-256 depuis la release officielle, vérifie la taille et l’intégrité, puis lance
le setup avec demande d’autorisation administrateur Windows. Le travail non
sauvegardé est protégé avant la fermeture et l’application redémarre après
installation. Les coffres et le rattachement paroissial sont conservés.
Un échec de recherche, de vérification ou un refus UAC laisse la version courante
utilisable. Le setup installe par défaut dans `C:\Program Files\Missanye`.

### Publier une version détectable

Les simples artefacts de compilation GitHub Actions ne sont pas des releases.
Le workflow Windows continue à produire ces artefacts pour les branches et PR.
Pour une livraison stable, le responsable de livraison peut créer un tag sur le
commit revu, par exemple :

```sh
git tag desktop-v0.2.0 <commit-revu>
git push origin desktop-v0.2.0
```

Le workflow :

1. Valide la version numérique du tag et compile le client avec cette version.
2. Construit l’image Windows et l’installateur Inno Setup de même version.
3. Vérifie l’installation et la désinstallation dans Windows CI.
4. Publie ensuite la release avec l’EXE et son SHA-256.

La permission d’écriture GitHub est limitée au job de publication, exécuté
uniquement pour ces tags et après réussite du job Windows. Ce changement ne crée
aucun tag ni release à lui seul. Tant qu’aucun installateur plus récent n’est
publié, l’application n’annonce pas de nouvelle version.

Pour un build manuel : `scripts/package.ps1 -AppVersion 0.2.0`, puis
`scripts/installer.ps1 -AppVersion 0.2.0`. Sous Linux, le script de paquet utilise
`MISSANYE_APP_VERSION`. Les tests d’interface désactivent les recherches automatiques
externes via `-Dmissanye.disableAutomaticUpdateChecks=true` ; le comportement
normal les active par défaut.
