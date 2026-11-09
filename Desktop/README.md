# Missanye Desktop — JavaFX MVC

Le module se trouve dans **`Desktop/`**, à côté de `Backend/` et `Frontend/`.
Il communique avec le backend Spring Boot ; seul ce dernier accède à Neon.

## Vues FXML

Les fichiers se trouvent dans `src/main/resources/com/missanye/desktop/view/` :

- `login-form.fxml` : connexion en ligne et coffre local, inspirée du formulaire
  `login_form.fxml` fourni dans `Church_Project.zip` (champs à droite).
- `workbench.fxml` : structure principale, sélection de paroisse et onglets.
- `request-form.fxml` : formulaire d'intention, forfaits, coordonnées et créneaux.
- `demands.fxml` : liste des demandes, décisions, paiements et reçus.
- `outbox.fxml` : brouillons et opérations en attente de synchronisation.
- `desktop.css` : palette, cartes, champs et boutons communs.

Ces fichiers sont adaptés à JavaFX 17 et peuvent être édités avec Scene Builder
compatible. Le contrôleur existant conserve les actions métier ; les FXML ne
contiennent ni SQL ni identifiants de connexion. Les classes de connexion à la
base de l'archive de référence ne sont pas importées.

## Lancer

Prérequis : **JDK 17**, **Maven 3.9**, environnement graphique ; GTK 3 sous Linux.

```sh
cd Desktop
mvn verify
mvn javafx:run
```

Le serveur doit inclure la migration **V31** et l'endpoint
`POST /desktop/operations` livrés avec ce module. Déployer d'abord ce backend.
Flyway applique V31 sans réinitialiser les données existantes. Le fichier
`render-neon.yaml` reste utilisable ; aucune nouvelle connexion directe à Neon
n'est nécessaire dans le desktop.

## Première utilisation

1. Indiquer l'URL **racine** de l'API, par exemple `http://localhost:8081` en
   développement, ou son URL HTTPS en production. Ne pas ajouter `/auth` ni le
   préfixe `/api` du proxy Vite. Les redirections ne sont pas suivies.
2. Saisir un compte serveur existant et son mot de passe. Choisir séparément un
   **mot de passe local d'au moins 12 caractères** pour chiffrer le coffre.
   Ce mot de passe local est indispensable pour rouvrir les données ; il n'existe
   pas de récupération intégrée. Les mots de passe serveur ne sont pas stockés.
3. Cliquer **Connexion en ligne**, sélectionner une paroisse autorisée puis
   **Actualiser catalogue et demandes**. Le catalogue et les demandes sont mis
   en cache dans le coffre de ce compte et de ce serveur.
4. Pour les ouvertures suivantes sans réseau, renseigner la même URL, le même
   identifiant et le mot de passe **local**, puis **Déverrouiller hors ligne**.
   Ce déverrouillage permet l'accès local ; les droits serveur seront revérifiés
   avant chaque synchronisation. Pour reconnecter, verrouiller puis se connecter
   en ligne ; utiliser toujours le même mot de passe local.

## Demandes et synchronisation

Dans **Nouvelle demande**, choisir type, forfait, horaires, mode de paiement et
remplir les coordonnées ainsi que l'intention. Pour un forfait multi-célébrations,
indiquer les dates supplémentaires séparées par des virgules au format
`AAAA-MM-JJ` ; le nombre total doit correspondre au forfait. Par défaut, un même horaire
ou une même heure personnalisée est utilisé pour ces dates. Le bouton
**Configurer chaque date et horaire** permet de choisir des créneaux distincts. Le serveur revalide
les jours, délais, tarifs et disponibilités à l'envoi.

Sauvegarder crée un brouillon chiffré. Dans **Brouillons et synchronisation**,
sélectionner ce brouillon et cliquer **Mettre en file d'envoi**. Une opération en
attente est immuable. **Synchroniser** envoie la file ; en ligne, le mode
automatique le fait également toutes les 60 secondes, avec temporisation
progressive après les erreurs réseau. La file garde le même UUID après une
coupure ou un redémarrage. L'identifiant est dédupliqué transactionnellement
par le serveur pour ce compte.

Un rejet métier reste visible avec sa cause. **Modifier / corriger** un rejet
de demande prépare un nouveau brouillon et un nouvel identifiant ; archiver le
rejet initial si nécessaire. Un conflit d'encaissement ne doit pas être rejoué
aveuglément : actualiser le serveur et rapprocher la caisse avant de décider.
Les erreurs de droits ou de session arrêtent l'envoi et exigent une reconnexion.

La liste des demandes est une **copie datée par sa dernière actualisation**,
pas une preuve d'état actuel hors ligne. L'actualisation remplace la copie
après téléchargement de toutes les pages, ce qui retire les demandes supprimées
sur le serveur. Elle ne modifie pas les brouillons ni la file locale.

## Modifier les demandes serveur

Dans **Demandes et paiements**, sélectionner une demande puis utiliser
**Corriger l'intention**, **Valider / rejeter** ou **Supprimer la demande**,
selon les permissions du compte. Ces opérations peuvent être mises en attente
hors ligne. Elles portent la version serveur consultée ; si le web ou un autre
poste modifie la demande entre-temps, le serveur rejette la commande et conserve
les données les plus récentes. Actualiser et rapprocher le conflit avant de
soumettre une nouvelle opération. Une suppression ne fait disparaître la ligne
locale qu'après confirmation serveur et actualisation de la liste.

## Paiements et reçus

- **Espèces** : sélectionner une demande serveur, confirmer le montant reçu.
  L'encaissement est provisoire dans la file jusqu'à sa confirmation serveur.
  Le serveur refuse un montant changé, un paiement déjà effectué ou des droits
  insuffisants. Deux postes ne peuvent pas confirmer deux fois la même commande.
- **Paiement en ligne** : le desktop demande une URL au backend puis ouvre le
  navigateur pour FedaPay. Cliquer **Vérifier le paiement** après le parcours.
  Internet et la configuration FedaPay serveur sont nécessaires ; aucune
  confirmation électronique n'est inventée hors ligne.
- **Reçus** : sélectionner une demande puis **Exporter / ouvrir le reçu PDF**.
  En ligne, le PDF officiel est téléchargé et mis en cache chiffré. Hors ligne,
  seuls les reçus déjà téléchargés sont disponibles. Le fichier exporté est un
  PDF ordinaire **non chiffré** ; l'impression se fait depuis le lecteur PDF.

## Stockage et sauvegarde

Le coffre est `~/.missanye-desktop/workflow.sqlite`. Les contenus, réponses,
PDF et catalogues sont chiffrés en AES-256-GCM ; la clé est dérivée du mot de
passe local avec PBKDF2-HMAC-SHA256 et un sel aléatoire propre au profil.
Les profils sont séparés par serveur et compte, et la synchronisation compare
l'identifiant serveur du propriétaire à la session courante. Les cookies ne
vivent qu'en mémoire. Le coffre doit rester sur un compte système personnel.

Pour changer le répertoire sous Linux/macOS :

```sh
JAVA_TOOL_OPTIONS="-Dmissanye.dataDir=/chemin/vers/donnees" mvn javafx:run
```

Sauvegarder le fichier SQLite après verrouillage de l'application et conserver
séparément le mot de passe local. Ne pas copier une base ouverte pendant une
écriture. Le prototype précédent `drafts.sqlite`, non chiffré, n'est ni importé
ni supprimé automatiquement ; il n'est plus utilisé.

## Construire une application autonome

Le paquet contient Java, JavaFX et les dépendances ; Java n'a pas besoin d'être
installé sur le poste destinataire. Construire sur le système et l'architecture
cibles avec un JDK 17 qui fournit `jpackage`.

```sh
# Linux / macOS
bash scripts/package.sh
```

```powershell
# Windows PowerShell
./scripts/package.ps1
```

Résultat : `target/distribution/Missanye/`, application autonome (`app-image`).
Sous Linux, lancer `target/distribution/Missanye/bin/Missanye` ; sous Windows,
l'exécutable est `Missanye.exe`. Archiver **tout le dossier**, pas seulement
l'exécutable. Si une distribution précédente existe, la déplacer avant de
reconstruire. Les scripts ne suppriment pas une distribution existante.

Pour obtenir le ZIP Windows via GitHub : après publication des fichiers du
projet, ouvrir **Actions → Desktop Windows → Run workflow**. Une fois le job
terminé, télécharger l'artefact **Missanye-Windows**, extraire le ZIP puis ouvrir
`Missanye/Missanye.exe`. Le workflow vérifie aussi les tests sur Windows.

L'image Linux a été construite ici ; les paquets Windows/macOS doivent être
construits et vérifiés sur leurs systèmes. Il n'y a pas encore de signature
numérique de l'application ni de système de mise à jour automatique.

## Installateur Windows Inno Setup

Le workflow **Desktop Windows** produit aussi l'artefact
**Missanye-Setup-Windows**, contenant **`Missanye-Setup.exe`** et son empreinte
SHA-256. Télécharger l'artefact depuis GitHub Actions, extraire l'archive puis
exécuter `Missanye-Setup.exe`. Java et JavaFX sont inclus.

L'installation est destinée à Windows 10/11 compatible x64 et se fait pour le
compte courant, sans demande de droits administrateur, dans
`%LOCALAPPDATA%\Programs\Missanye` par défaut. L'assistant propose le français
et l'anglais, un raccourci menu Démarrer et un raccourci Bureau optionnel.
La désinstallation préserve le coffre `~/.missanye-desktop` et ses données.
Les futures versions doivent conserver le même `AppId` Inno Setup.

Pour compiler l'installateur sur un poste Windows, installer **Inno Setup 6**
depuis [son site officiel](https://jrsoftware.org/isinfo.php), puis exécuter :

```powershell
cd Desktop
./scripts/package.ps1
./scripts/installer.ps1
```

Résultat : `Desktop/target/installer/Missanye-Setup.exe`. Si le compilateur n'est
pas dans son emplacement habituel, préciser son chemin :

```powershell
./scripts/installer.ps1 -IsccPath 'C:\outils\Inno Setup 6\ISCC.exe'
```

La version peut être définie avec `-AppVersion 0.1.1` ; garder la version de
`package.ps1` et du projet Maven cohérente lors d'une nouvelle livraison.
Le workflow utilise Inno Setup présent sur le runner Windows, vérifie les
fichiers installés (exécutable, JAR et runtime Java), puis la désinstallation.
La compilation et ces vérifications nécessitent Windows ; une validation YAML
sur Linux ne prouve pas que l'installateur a été produit.

## Vérifications

`mvn verify` exécute les tests SQLite chiffrés, isolation des comptes, reprise
après réponse incertaine, UUID stable, rejets, session expirée, cookies HTTP et
interaction JavaFX headless. Le test réel `LiveBackendTest` est normalement
ignoré : il doit s'exécuter uniquement contre une base **jetable**, initialisée
par `ApplicationStartupSmokeTest` du backend.

```sh
DESKTOP_E2E=true DESKTOP_API_URL=http://127.0.0.1:18081 mvn verify
```

Ce test utilise exclusivement le compte de test créé par la suite backend. Il
crée une demande, simule son encaissement et télécharge un reçu. Ne pas le lancer
contre la production. Le paiement FedaPay réel reste à valider avec un compte
sandbox configuré ; aucun paiement externe n'est exécuté par les tests.
