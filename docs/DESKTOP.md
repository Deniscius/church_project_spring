# Architecture desktop livrée

Le code est dans `Desktop/` ; les instructions sont dans `Desktop/README.md`.

```text
React web ──────────────────────┐
                               ├── HTTPS / JSON ── Spring Boot ── PostgreSQL / Neon
JavaFX MVC ↔ coffre SQLite ─────┘
             chiffré + outbox
```

Les postes initient les échanges vers le serveur ; ils n'exposent pas d'API
HTTP entrante et ne connaissent pas les identifiants Neon. Le web et le desktop
partagent les règles métier et les autorisations Spring Boot existantes.

## Organisation

| Couche | Emplacement | Responsabilité |
|---|---|---|
| Modèles | `Desktop/.../model/` | Choix de catalogue et lignes du poste de travail |
| Vue | `Desktop/.../view/WorkbenchView.java` | Composants JavaFX natifs, onglets et formulaires |
| Contrôleur | `Desktop/.../controller/WorkbenchController.java` | Navigation et orchestration asynchrone des actions |
| Stockage | `Desktop/.../repository/LocalStore.java` | Coffre SQLite chiffré, cache, brouillons et file persistante |
| API | `Desktop/.../api/ApiClient.java` | HTTP, cookies en mémoire, erreurs et PDF |
| Synchronisation | `Desktop/.../service/SyncService.java` | Envoi, reprise avec le même UUID, validation de session et rafraîchissement |
| Serveur | `Backend/.../service/desktop/DesktopOperationService.java` | Autorisation, déduplication et mutation transactionnelle |

Les vues sont écrites en Java, sans FXML. Les opérations réseau et SQLite passent
par un exécuteur séquentiel ; l'affichage et les interactions restent sur le
thread JavaFX. Le stockage local ne remplace pas les règles métier serveur.

## Contrats

Authentification : `/auth/login-multi-tenant`, `/auth/me`, `/auth/logout`.
Les JWT restent dans les cookies, jamais dans SQLite. Les mutations incluent
`X-Requested-With: XMLHttpRequest`, comme l'exige le backend existant.

`POST /desktop/operations` accepte un UUID stable, un type et un contenu :
création, correction d'intention, décision de validation, suppression ou
encaissement en espèces. La migration V31 ajoute une clé unique utilisateur / UUID.
La prise de l'identifiant, l'effet métier et la réponse sont dans la même
transaction JDBC/JPA. Un contenu différent sous le même identifiant est refusé.
Les droits et l'accès à la paroisse sont vérifiés avant un envoi ou un rejeu.

Les modifications et suppressions portent la date de modification attendue.
Après verrouillage de la demande, le serveur rafraîchit l'entité et compare sa
version. En cas de conflit, la commande est rejetée sans écraser le changement
web. Les encaissements sont sérialisés sur la ligne demande et comparent aussi
le montant attendu. Les règles existantes interdisent un paiement déjà effectué.

La liste serveur est téléchargée page par page et remplace entièrement la copie
locale uniquement après un téléchargement réussi. Cela prend en compte les
suppressions sans ajouter de protocole de curseurs. Cette stratégie convient à
une première version paroissiale ; pour de très gros volumes, un protocole
incrémental avec marqueurs de suppression sera plus adapté.

## Hors ligne

Première connexion obligatoirement en ligne. Les ouvertures suivantes utilisent
un mot de passe local séparé, dont est dérivée la clé AES-GCM du coffre. Les
sessions locales sont séparées par serveur, compte et propriétaire UUID.
La reprise vérifie de nouveau la session et les autorisations serveur.

Les brouillons, commandes en attente et confirmations sont des états distincts.
Une réponse incertaine laisse la commande en attente avec son UUID initial.
Les rejets sont conservés ; une demande corrigée reçoit un nouvel UUID. Les
conflits financiers demandent un rapprochement explicite. Les coupures ne
transforment jamais une opération locale en paiement confirmé.

Les reçus déjà téléchargés sont lisibles hors ligne depuis le cache chiffré.
L'export vers un lecteur PDF crée un fichier ordinaire non chiffré. FedaPay
nécessite une connexion et les clés configurées côté serveur ; le desktop ouvre
le parcours de paiement dans le navigateur et vérifie ensuite le statut serveur.

## Livraison

Les scripts `Desktop/scripts/package.ps1` et `package.sh` produisent une image
applicative autonome via `jpackage`. Le workflow `Desktop Windows` construit un
ZIP contenant `Missanye.exe` et son runtime, disponible comme artefact GitHub
Actions après publication du code et exécution du workflow sur Windows.
Le script `Desktop/scripts/installer.ps1` compile ensuite
`Desktop/installer/Missanye.iss` avec Inno Setup pour générer
`Missanye-Setup.exe`. Le workflow publie cet installateur dans l'artefact
`Missanye-Setup-Windows` après un test d'installation/désinstallation silencieux.
Le coffre utilisateur est conservé par la désinstallation.

Les tests couvrent le stockage chiffré, l'isolation, la reprise, les rejets,
les cookies et une interaction JavaFX rendue en headless. La suite backend
vérifie le rejeu et la concurrence sur PostgreSQL. Un test desktop optionnel
exerce le backend local réel, les espèces, les conflits d'intention et les PDF.
La validation d'un paiement externe et des paquets Windows se fait dans leurs
environnements respectifs ; elle n'est pas établie par un test headless Linux.
