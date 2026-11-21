# Recette SasPay — sandbox puis décision de mise en service

Toutes les lignes ci-dessous sont **non vérifiées** tant qu'un humain n'a pas
exécuté le scénario avec son marchand et consigné la preuve. Les tests PostgreSQL
et HTTP simulés du dépôt ne valident pas la connectivité SasPay ni un paiement
sandbox réel. Ne jamais coller de clé, de secret, de code de suivi ou de données
personnelles dans un ticket ou le compte rendu.

Référence consultée le 21 novembre 2026 :
<https://docs.saspay.me/api-reference/openapi.json>. Elle distingue les serveurs
sandbox `https://sandbox-api.saspay.me/api/v1` et live
`https://api.saspay.me/api/v1`. Les routes utilisées sont celles de la spécification,
pas des routes softpay ou de reversement automatique.

## Préparation

Utiliser un déploiement isolé avec des demandes fictives, une base de test, un
marchand sandbox validé et une clé de test autorisée à encaisser (`PAYIN` ou
`BOTH`). Ne jamais lancer cette recette sur la production par défaut.

Charger les secrets dans un terminal privé, sans les placer dans la commande ni
activer `set -x` :

```bash
read -rsp 'Clé API SasPay de test : ' SASPAY_API_KEY; echo
export SASPAY_API_KEY
read -rsp 'Secret du webhook sandbox : ' SASPAY_WEBHOOK_SECRET; echo
export SASPAY_WEBHOOK_SECRET
export SASPAY_COUNTRY=TG
export SASPAY_API_BASE_URL=auto # La clé de test sélectionne sandbox-api.saspay.me
# Remplacer par les URL HTTPS du déploiement isolé :
export APP_URL=https://votre-front-test.example
export BACKEND_URL=https://votre-api-test.example
```

Saisir les mêmes variables sur le **backend de test**, puis `SASPAY_ENABLED=true`
et redéployer ce backend. Les variables du terminal seules ne configurent pas
Render. Créer dans le tableau de bord SasPay sandbox un webhook vers
`BACKEND_URL/webhooks/saspay` avec le même secret. Ne placer aucune de ces clés
dans une variable `VITE_*` ni dans Desktop.

| Contrôle préalable | État | Preuve à conserver sans secret |
| --- | --- | --- |
| Projet, base et données de test isolés de la production | non vérifié | noms des environnements |
| Mode sandbox, marchand validé, clé d'encaissement et webhook configurés | non vérifié | date et résultat, sans capture des secrets |
| `python3 Backend/scripts/check_saspay.py --offline` | non vérifié | code de sortie et noms des variables manquantes éventuelles |
| `python3 Backend/scripts/check_saspay.py` | non vérifié | lectures `countries/` et `pricing/my-rates/` réussies |
| `GET /pricing/my-rates/` : réseaux TG réellement disponibles et tarifs | non vérifié | noms des réseaux et frais attendus |
| Frais `ADD_ON` et autorisation `ALLOW_CLIENT_OVERRIDE` | non vérifié | réglage marchand |
| `/actuator/health` retourne `UP` | non vérifié | date et HTTP 200 |

`check_saspay.py` n'initie aucun paiement. Une erreur d'authentification, un réseau
indisponible ou une liste de tarifs ambiguë bloque la suite. L'API actuelle ne
prend pas en charge `Idempotency-Key` sur `POST /checkout-sessions/` : ne pas
relancer une création après un timeout. Examiner `UNCERTAIN` et retrouver la
session par `metadata.missanye_reference` avant toute décision.

## Demande de messe

Créer une demande fictive éligible, avec première célébration future et e-mail de
test. Utiliser le mode de paiement en ligne ; les espèces ne font pas partie de
cette recette. Garder les références uniquement dans le dossier privé de test.

| Scénario et résultat attendu | État | Preuve à consigner |
| --- | --- | --- |
| « Payer avec SasPay » ouvre la page hébergée officielle en sandbox, avec montant et devise XOF attendus | non vérifié | référence locale privée, montant et état |
| Retour navigateur avant confirmation : aucun passage à payé sur la seule base de l'URL ou de `PENDING` | non vérifié | état facture et demande avant confirmation |
| Paiement simulé réussi : le backend vérifie la session puis la transaction `SUCCESS`, l'ID, la devise, le montant et le net | non vérifié | état `saspay_checkout=SUCCESS` et montants canoniques |
| Reçu payé disponible et facture/demande payées | non vérifié | résultat de consultation du PDF |
| Crédit paroissial unique égal au prix de la messe, hors commission plateforme | non vérifié | une écriture `CREDIT_MESSE`, référence `SASPAY:<transaction>` et variation du solde |
| Paiement échoué sur une autre demande : aucun reçu portant la mention payé, aucun crédit | non vérifié | états facture, demande et compte |
| Paiement annulé sur une autre demande : aucun reçu payé, aucun crédit | non vérifié | états facture, demande et compte |
| Fermeture du navigateur après paiement : le webhook/polling retrouve le succès sans dépendre du retour web | non vérifié | rapprochement final |

Un PDF non réglé peut exister avant paiement : vérifier sa **mention de paiement**,
pas seulement la présence d'un fichier.

## Abonnement

Utiliser une paroisse et un abonnement de test. Noter l'échéance initiale et la
durée contractuelle de la période choisie avant de payer.

| Scénario et résultat attendu | État | Preuve à consigner |
| --- | --- | --- |
| La demande d'abonnement ouvre un checkout sandbox avec le montant contractuel | non vérifié | référence privée et montant |
| Succès vérifié : un abonnement actif et une seule prolongation de la durée contractuelle | non vérifié | `fin_at`, `subscription_expires_at`, source `SASPAY` |
| Échec sur une autre période : aucune activation ni prolongation | non vérifié | statut et échéance inchangée |
| Annulation sur une autre période : aucune activation ni prolongation | non vérifié | statut et échéance inchangée |
| Réouverture du retour signé et nouvelle vérification du même paiement : échéance inchangée | non vérifié | comparaison des échéances |

## Notifications et rejeu

Depuis le tableau de bord SasPay **sandbox**, envoyer l'événement `webhook.test`
vers le backend de test et consulter son journal de livraison. Pour un paiement
sandbox effectivement réussi, relivrer `transaction.success` si le tableau de
bord le permet. En complément, le script suivant peut rejouer une notification
minimale signée par le secret du backend de test : il teste le traitement du
backend, **pas l'émission de la notification par SasPay**.

```bash
read -rp 'UUID de transaction sandbox déjà réussie : ' SASPAY_TEST_TRANSACTION_ID
export SASPAY_TEST_TRANSACTION_ID
python3 - <<'PY'
import hashlib, hmac, json, os, time, urllib.request, uuid
transaction = str(uuid.UUID(os.environ['SASPAY_TEST_TRANSACTION_ID']))
body = json.dumps({'event': 'transaction.success', 'data': {'id': transaction}},
                  separators=(',', ':')).encode()
url = os.environ['BACKEND_URL'].rstrip('/') + '/webhooks/saspay'
assert url.startswith('https://'), 'Backend HTTPS requis'
for _ in range(2):
    timestamp = str(int(time.time()))
    signature = hmac.new(os.environ['SASPAY_WEBHOOK_SECRET'].encode(),
                         timestamp.encode() + b'.' + body, hashlib.sha256).hexdigest()
    request = urllib.request.Request(url, data=body, method='POST', headers={
        'Content-Type': 'application/json', 'X-Webhook-Event': 'transaction.success',
        'X-Webhook-Timestamp': timestamp, 'X-Webhook-Signature': signature})
    with urllib.request.urlopen(request, timeout=15) as response:
        print('HTTP', response.status)
PY
```

Répéter ce contrôle pour la transaction de la demande puis celle de l'abonnement.
Comparer les écritures et échéances après le rapprochement effectif ; un simple
ACK du webhook ne prouve pas l'encaissement. Ne pas soumettre de fausse réussite
pour une transaction non payée : le backend doit toujours vérifier chez SasPay.

| Contrôle | État | Preuve à consigner |
| --- | --- | --- |
| `webhook.test` depuis SasPay reçoit un ACK et ne crédite rien | non vérifié | statut de livraison |
| Deux livraisons identiques `transaction.success` restent dédupliquées | non vérifié | une ligne par `(transaction_id,event_type)` |
| Rejeu demande : aucun second crédit ni doublon de paiement | non vérifié | écriture et solde inchangés |
| Rejeu abonnement : aucune seconde période ni nouvelle prolongation | non vérifié | échéances inchangées |
| Signature invalide ou corps modifié : HTTP 401, aucune notification persistée | non vérifié | code HTTP et absence d'effet |
| Signature correcte mais timestamp de plus de 300 secondes : HTTP 401, aucun effet | non vérifié | code HTTP et absence d'effet |
| Montant/net incompatibles : `REVIEW`, aucun crédit ni activation automatique | non vérifié | preuve sur un cas sandbox prévu par le prestataire ; sinon laisser non vérifié |

Ne pas modifier des montants en base pour simuler un succès financier. Les
incompatibilités sont couvertes séparément par les tests locaux automatisés ;
un scénario que le prestataire ne permet pas de produire reste non vérifié ici.

## Surveillance et contrôle préalable au retrait historique

Lecture sur la base correspondant explicitement au déploiement contrôlé :

```sql
SELECT id,purpose,target_id,state,remote_id,created_at
FROM saspay_checkout
WHERE state IN ('REVIEW','UNCERTAIN') ORDER BY created_at;

SELECT count(*) FROM details_paiement
WHERE provider = 'FEDAPAY' AND statut_paiement = 'EN_ATTENTE';
```

Le second comptage a retourné **0 en production le 21 novembre 2026**, sur le
projet Neon `missanye`, branche `production`, hôte correspondant au backend
Render. Aucune ligne n'a été modifiée. C'est un constat à cette date, pas une
garantie pour une mise en production ultérieure. Le refaire juste avant la
bascule ; un résultat positif impose de garder l'ancienne version et de rapprocher
les paiements avant le retrait. Vérifier aussi les abonnements historiques en
attente dans l'interface comptable avant la bascule.

| Contrôle | État | Preuve à consigner |
| --- | --- | --- |
| Aucun `REVIEW` ou `UNCERTAIN` inexpliqué | non vérifié | nombre et décisions de rapprochement |
| Nouveau comptage historique avant déploiement et contrôle des abonnements en attente | non vérifié | date et résultat |

Ne jamais purger une session ambiguë pour forcer un nouveau POST. Retrouver la
transaction dans le tableau de bord, vérifier ses montants et son résultat,
puis décider explicitement du rapprochement ; conserver les preuves historiques.

## Passage en live — décision humaine

| Contrôle | État | Preuve à consigner |
| --- | --- | --- |
| Recette sandbox demande et abonnement validée | non vérifié | date et responsable |
| Clé live, webhook live et URL API live configurés sur le backend seulement | non vérifié | environnement, sans secrets |
| Premier essai réel avec un petit montant autorisé, puis rapprochement et reçu | non vérifié | montant, résultat et responsable |
| Activation générale seulement après cet essai | non vérifié | décision explicite |

La clé live se configure dans le gestionnaire de secrets, jamais dans ce document.
Le diagnostic live exige explicitement `python3 Backend/scripts/check_saspay.py
--allow-live`. Il reste en lecture seule. Aucun basculement live ni paiement réel
n'a été effectué par l'agent.
