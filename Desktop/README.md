# Missanye Desktop — JavaFX MVC

Le module se trouve dans **`Desktop/`**, à côté de `Backend/` et `Frontend/`.
Il communique avec le backend Spring Boot ; seul ce dernier accède à Neon.

## Vues FXML

Les fichiers se trouvent dans `src/main/resources/com/missanye/desktop/view/` :

- `login-form.fxml` : connexion habituelle au compte de la paroisse, inspirée du formulaire
  `login_form.fxml` fourni dans `Church_Project.zip` (champs à droite).
- `workbench.fxml` : navigation principale et identité de la paroisse.
- `parish-deployment.fxml` : vérification administrateur et rattachement du poste.
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

## Présentation et résolutions

L'application s'ouvre sur un tableau de bord après connexion. Le menu latéral
permet d'accéder à la saisie, aux demandes et à l'activité sans perdre les champs
en cours. Il se réduit à des icônes sur les petites largeurs. Les cartes, les
barres d'actions et les tableaux s'adaptent à la place disponible.

Les vues sont vérifiées de 900 × 600 à 1920 × 1080 unités JavaFX. Voir
[la conception et les vérifications de l'interface](../docs/DESKTOP_INTERFACE.md).
Le workflow Windows publie aussi les captures **Missanye-Interface-Previews**.

## Première utilisation

Le développeur rattache une fois le poste à la paroisse via **Configurer ce
poste**, avec le compte administrateur de la plateforme. Les comptes et leurs
accès sont configurés sur le web.

L'utilisateur saisit ensuite son **identifiant et son mot de passe habituels**.
L'application ouvre directement la paroisse du poste, avec les permissions
renvoyées par le service. Aucun choix de paroisse, mode de connexion ou second
mot de passe ne fait partie du parcours normal.

Voir [le déploiement par paroisse, le RBAC et la protection Windows](../docs/DESKTOP_PAROISSE.md)
pour produire un installateur préconfiguré et migrer les anciens brouillons.

La barre de session remplace le formulaire une fois connecté. Les tableaux
proposent une recherche des demandes, un filtre de l'activité et des états
lisibles. Les actions indisponibles sont désactivées selon la sélection, les
droits et le mode de connexion. Les compteurs reflètent les données du poste.

## Configuration réservée au développeur qui déploie

La destination de l'API n'est pas un réglage de l'utilisateur final. Elle est
intégrée dans le paquet à la compilation via la propriété Maven
`desktop.api.url`. Sa valeur par défaut est :

```text
https://church-project-spring-pmbz.onrender.com
```

Pour livrer vers un autre environnement sur Windows :

```powershell
./scripts/package.ps1 -ApiBaseUrl 'https://api.environnement.example'
./scripts/installer.ps1
```

Pour les builds Maven ou le développement local :

```sh
mvn verify -Ddesktop.api.url=https://api.environnement.example
mvn javafx:run -Ddesktop.api.url=http://localhost:8081
```

Le script shell accepte `MISSANYE_BUILD_API_URL`. Le fichier embarqué
`com/missanye/desktop/config/deployment.properties` est filtré par Maven. Pour
un lancement administré, `-Dmissanye.apiUrl=...` (option JVM) ou
`MISSANYE_API_URL` peut surcharger cette valeur, dans cet ordre. Une adresse
enregistrée depuis **Configurer ce poste** est prioritaire sur ces valeurs.
Les URL doivent être absolues, sans
identifiant, query string ou fragment ; HTTPS est obligatoire sauf en local.
Aucune clé Neon ou FedaPay ne doit être ajoutée à ce fichier.

### Configuration de la paroisse sur le poste

**Configurer ce poste** vérifie l'API et les identifiants du `SUPER_ADMIN` global,
puis propose les paroisses actives. Le choix est enregistré sans mot de passe
ni cookie. Les utilisateurs ordinaires n'éditent pas le rattachement.
Le profil peut être inclus dans l'installateur avec `-DeploymentFile` : voir
[DESKTOP_PAROISSE.md](../docs/DESKTOP_PAROISSE.md).

La séparation des coffres utilise l'adresse canonique du service et le compte.
Changer d'environnement crée un espace local distinct. Avant de remplacer une
adresse de production, le développeur doit prévoir la reprise des opérations
encore présentes dans les coffres de l'ancien environnement.

## Demandes et synchronisation

Dans **Nouvelle demande**, choisir type, forfait, horaires, mode de paiement et
remplir les coordonnées ainsi que l'intention. Pour un forfait multi-célébrations,
ajouter les dates supplémentaires avec le calendrier ; le nombre total doit
correspondre au forfait. Une date peut être retirée depuis la liste. Par défaut, un même horaire
ou une même heure personnalisée est utilisé pour ces dates. Le bouton
**Configurer chaque date et horaire** permet de choisir des créneaux distincts. Le serveur revalide
les jours, délais, tarifs et disponibilités à l'envoi.

Sauvegarder crée un brouillon chiffré, même si des champs ne sont pas encore
complétés. Les vérifications obligatoires interviennent avant la transmission.
Un changement de paroisse, une déconnexion, un nouveau formulaire ou une fermeture
normale propose de conserver, abandonner ou annuler la saisie non enregistrée.
Une fermeture est différée si une opération est en cours.

Dans **Mon activité**,
sélectionner ce brouillon et cliquer **Transmettre la demande**. Une opération en
attente peut revenir au brouillon tant qu'aucune tentative d'envoi n'a commencé
(bouton **Revenir au brouillon**). Après une tentative, elle reste immuable pour
préserver sa protection contre les doublons. Les encaissements ne peuvent pas
être remis en brouillon de cette façon.

**Transmettre les opérations** envoie la file ; en ligne, le mode
automatique le fait également toutes les 60 secondes, avec temporisation
progressive après les erreurs réseau. La file garde le même UUID après une
coupure ou un redémarrage. L'identifiant est dédupliqué transactionnellement
par le serveur pour ce compte.

Un rejet métier reste visible avec sa cause. **Modifier le brouillon** un rejet
de demande prépare un nouveau brouillon et un nouvel identifiant ; archiver le
rejet initial si nécessaire. Une correction enregistrée conserve l'ancien rejet
avec l'état **Remplacée** et ne peut pas être créée deux fois depuis ce rejet.
Un conflit d'encaissement ne doit pas être rejoué
aveuglément : actualiser le serveur et rapprocher la caisse avant de décider.
Une session expirée arrête l'envoi et exige une reconnexion. Un refus de droits
sur une opération la marque **Bloquée** sans interrompre les autres. Après
rétablissement des accès, **Réessayer la transmission** conserve son identifiant
initial ; une opération bloquée ne doit pas être recopiée comme un nouvel
encaissement.

Les actualisations conservent les champs et les choix d'une demande en cours,
ainsi que la sélection des lignes par leur identifiant. Le tri des tableaux
fonctionne avec les filtres ; les montants et les dates se trient selon leur
valeur, et non selon leur texte affiché.

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
  navigateur pour FedaPay. Cliquer **Vérifier le règlement** après le parcours.
  Internet et la configuration FedaPay serveur sont nécessaires ; aucune
  confirmation électronique n'est inventée hors ligne.
- **Reçus** : sélectionner une demande puis **Télécharger le reçu**.
  En ligne, le PDF officiel est téléchargé et mis en cache chiffré. Hors ligne,
  seuls les reçus déjà téléchargés sont disponibles. Une coupure du service peut
  également utiliser cette copie conservée, signalée à l'utilisateur comme telle. Le fichier exporté est un
  PDF ordinaire **non chiffré** ; l'impression se fait depuis le lecteur PDF.

## Stockage et sauvegarde

Le coffre est `~/.missanye-desktop/workflow.sqlite`. Les contenus, réponses,
PDF et catalogues sont chiffrés en AES-256-GCM. La clé aléatoire du nouveau
parcours est protégée par Windows DPAPI. Les anciens coffres dérivés d’un mot de
passe local sont migrés sans effacer leur contenu.
Les profils sont séparés par serveur et compte, et la synchronisation compare
l'identifiant serveur du propriétaire à la session courante. Les cookies ne
vivent qu'en mémoire. Le coffre doit rester sur un compte système personnel.

Pour changer le répertoire sous Linux/macOS :

```sh
JAVA_TOOL_OPTIONS="-Dmissanye.dataDir=/chemin/vers/donnees" mvn javafx:run
```

Sauvegarder après fermeture de l’application et préserver le profil Windows de
protection DPAPI, ou transmettre les opérations au service avant de changer de poste. Ne pas copier une base ouverte pendant une
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
Une seule instance de l'application peut ouvrir un même répertoire de données.
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

## Problème de connexion

Le desktop distingue un serveur trop lent, un service introuvable (DNS), un
serveur injoignable et une erreur de connexion sécurisée (TLS). Les messages ne
contiennent ni mot de passe ni détails internes renvoyés par le serveur.
Une erreur après le délai d'attente ne prouve pas que les identifiants sont faux :
une réponse HTTP 401 est signalée séparément. Ne pas désactiver la vérification
TLS pour contourner un problème de certificat.

Les messages de connexion distinguent un refus des identifiants par l'API et un
refus de session après authentification réussie. La paroisse affectée au poste
et le résultat apparaissent près des champs de connexion. Le compte créé par le
seeder utilise les mêmes identifiants que sur le web. Le compte administrateur global
peut préparer le rattachement ; si aucune paroisse active n’existe, il doit en
créer une depuis le web avant le déploiement.

Les dialogues sont redimensionnables et limités à la zone visible de l'écran,
avec défilement si nécessaire. La page de connexion masque le panneau décoratif
sur les petites largeurs et défile en hauteur. Les confirmations et boutons
standard sont présentés en français. L'éditeur d'intention est multiligne et
bloque une correction de longueur invalide ; la fenêtre de créneaux reste
ouverte quand une heure doit être corrigée.
