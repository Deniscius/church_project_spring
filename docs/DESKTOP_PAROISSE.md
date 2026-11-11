# Déploiement d'un desktop par paroisse

## Architecture et hébergement

Tous les postes appellent la même API Spring Boot et la même base PostgreSQL
Neon que le web. Chaque installation possède un rattachement à une paroisse.
Aucun serveur supplémentaire ni base Neon supplémentaire n'est créé pour une
paroisse ou un ordinateur. Les coûts restent ceux de l'infrastructure mutualisée
et du matériel ; ce choix ne garantit pas la gratuité des offres Render/Neon.

Le poste Windows conserve ses brouillons et sa copie de travail protégée. Il
n'expose aucune API locale et ne reçoit aucun identifiant de base de données.
Le web et le desktop consultent les mêmes informations serveur. Les modifications
locales en attente sont appliquées après confirmation par l'API.

## Comptes et RBAC

Les comptes sont créés et administrés sur le web. Pour chaque compte local à une
paroisse, l'administrateur configure le rôle applicatif, son activation et son
accès à la paroisse. Le backend impose une seule paroisse active aux comptes
locaux. Les comptes plateforme restent régis par les règles existantes.

Le desktop appelle les mêmes endpoints `auth/login-multi-tenant` et `auth/me`.
Il exige un accès à la paroisse du poste, puis utilise `user.permissions` renvoyé
par le backend. Il ne crée aucun rôle local et ne recalcule pas les droits à
partir des seuls noms de fonctions paroissiales.

| Capacité du desktop | Permission utilisée |
|---|---|
| Consultation de l'espace métier | `demand:read` et accès à la paroisse du poste |
| Saisie et correction des demandes | `demand:edit` |
| Décision sur une demande | `demand:validate` |
| Suppression d'une demande | `demand:delete` |
| Encaissement et actions de règlement | `payment:manage` |
| Téléchargement de reçus | `invoice:read` ou `receipt:manage` |

Les actions non autorisées sont masquées ou désactivées. L'API demeure l'autorité
finale pour chaque mutation et vérifie à nouveau les droits. Le desktop couvre
les demandes, paiements et reçus ; les écrans de gestion des comptes, abonnements
et référentiels complets du web restent sur le web.

## Activation du poste et déploiement

1. Installer **Missanye-Setup-Windows** sur le poste.
2. Ouvrir **Configurer ce poste** avant connexion.
3. Indiquer l’API commune et s’authentifier avec un compte autorisé. La liste
   contient les paroisses permises pour ce compte. Les identifiants ne sont pas
   enregistrés dans le profil de déploiement.
4. Choisir une paroisse active et cliquer **Rattacher ce poste**.
5. Les utilisateurs saisissent ensuite leurs identifiants web habituels.

Le nom de paroisse apparaît sur la connexion et dans l'espace métier. Les
utilisateurs ne choisissent pas une autre paroisse. Leurs droits serveur doivent
correspondre à celle de l'installation ; un compte d'une autre paroisse est
refusé. Modifier le rattachement exige à nouveau la vérification des accès au compte.

Le profil `server.properties` contient uniquement `api.base-url`, `parish.id` et
`parish.name`. Il se trouve par défaut sous `%USERPROFILE%\.missanye-desktop`.
Il peut être copié comme profil de déploiement : il ne contient ni mot de passe,
ni jeton, ni clé Neon. Pour produire un installateur déjà rattaché à une paroisse,
construire sur Windows à partir de ce profil validé :

```powershell
./scripts/package.ps1 -DeploymentFile 'C:\deploiement\saint-paul.properties'
./scripts/installer.ps1
```

Le profil est inclus dans l'application. Le même installateur peut être utilisé
sur les postes de cette paroisse. Chaque utilisateur Windows initialise sa propre
protection lors de sa première authentification. La configuration locale déjà
présente sur un poste reste prioritaire ; une mise à jour ne change pas
silencieusement sa paroisse. L'installateur actuel installe l'application pour
l'utilisateur Windows courant.

Pour le développement, le script shell accepte `MISSANYE_BUILD_DEPLOYMENT_FILE`.
Le paramètre JVM `missanye.deploymentFile` indique le profil embarqué ; le lanceur
jpackage le définit vers `$APPDIR/parish.properties`.

## Connexion naturelle et protection de la continuité

L'écran normal demande seulement l'identifiant et le mot de passe du compte.
Il ne propose ni choix de mode ni second mot de passe local. Une première
vérification avec le serveur est requise. La clé aléatoire du coffre AES-GCM est
protégée par Windows DPAPI pour l'utilisateur Windows courant. Les cookies et
le mot de passe serveur ne sont pas enregistrés. Le logo Missanye bleu et or est
fourni en PNG et en ICO pour la fenêtre, le lanceur et l'installateur.

Après une authentification et une préparation réussies, une empreinte PBKDF2
salée du mot de passe permet une ouverture de continuité pendant au maximum
72 heures lorsque le réseau ou le service est indisponible. Le même formulaire
est utilisé. Un refus HTTP 401/403, une erreur TLS ou une réponse invalide ne
déclenche pas cette ouverture de secours. Un retrait d'accès connu invalide
l'identité préparée. Sans réseau, une révocation distante ne peut être connue
immédiatement ; les permissions mises en cache restent bornées par cette durée.
Aucun encaissement n'est confirmé sans réponse du serveur.

DPAPI protège les données vis-à-vis des autres profils Windows. Plusieurs
personnes partageant un même profil Windows partagent cette frontière de
protection système ; utiliser des profils Windows distincts pour isoler leurs
données locales. Les clés DPAPI ne sont pas des fichiers de déploiement à copier
sur un autre poste. Copier le seul SQLite ne suffit pas à restaurer un coffre
sur un autre profil Windows : la sauvegarde doit préserver le profil de
protection, ou les données doivent avoir été transmises au service.

## Migration d'une installation existante

Si un coffre de l'ancienne version existe, le compte est d'abord vérifié auprès
du serveur. Un dialogue demande une seule fois l'ancien mot de passe local afin
de rattacher le coffre à DPAPI. Il ne recrée ni n'efface les brouillons et les
commandes. Après cette étape, la connexion normale utilise uniquement le mot de
passe du compte web. Sans l'ancien secret, la migration est annulée et le coffre
reste intact. Les opérations associées à une autre paroisse restent conservées
mais ne sont jamais transmises par l'installation rattachée à la paroisse
courante.

## Validation

Les tests couvrent le rattachement administrateur, le formulaire normal,
l'affichage d'un profil en consultation, le refus d'une autre paroisse,
la préparation protégée, la continuité limitée, les refus d'authentification,
les changements de mot de passe, la migration des anciens brouillons et le
filtrage des transmissions par paroisse. Le test DPAPI réel s'exécute sous
Windows dans le workflow ; les tests Linux utilisent un protecteur injecté
uniquement pour leurs fichiers jetables. Le parcours avec Spring Boot et
PostgreSQL réels teste aussi la connexion paroissiale en HTTPS.

Le parcours complet des demandes et reçus avant transmission est décrit dans
[DESKTOP_HORS_CONNEXION.md](DESKTOP_HORS_CONNEXION.md).

## Affectation du stockage cloud

La préparation à une future répartition des paroisses entre bases est décrite
dans [SHARDING.md](SHARDING.md). La base Neon reste partagée ; aucune adresse
de base ou affectation technique n’est demandée à l’utilisateur du desktop.
