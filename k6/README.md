# Tests K6 de production — Missanye

Ces scénarios sont volontairement **en lecture seule**. Ils ne créent ni demande,
ni paiement, ni e-mail et ne modifient aucune donnée de production.

## Cibles par défaut

- Frontend : `https://missanye-web.onrender.com`
- API : `https://missanye-api.onrender.com`

Les URLs peuvent être remplacées avec `WEB_URL`, `API_URL` et `WEB_ORIGIN`.

## 1. Parcours public et rate limiting

```bash
k6 run k6/read-only-production.js
```

Le filtre Spring Boot limite les lectures publiques à 60 requêtes/minute/IP.
Un générateur K6 local utilise une seule IP : des réponses HTTP 429 sont donc
attendues lorsque ce seuil est dépassé. Ce scénario valide à la fois les pages
publiques et le mécanisme anti-abus ; il ne doit pas servir seul à estimer la
capacité réelle avec des visiteurs provenant d'IP différentes.

## 2. Parcours administrateur authentifié

Ne jamais écrire les identifiants dans le script ou dans Git.

### PowerShell

```powershell
$env:K6_USERNAME = "compte-test"
$env:K6_PASSWORD = "mot-de-passe-temporaire"
k6 run k6/authenticated-read-production.js
Remove-Item Env:K6_USERNAME
Remove-Item Env:K6_PASSWORD
```

### Bash

```bash
K6_USERNAME='compte-test' \
K6_PASSWORD='mot-de-passe-temporaire' \
k6 run k6/authenticated-read-production.js
```

Le scénario conserve le cookie JWT HttpOnly comme un navigateur et teste :

- la session courante ;
- le profil ;
- la liste paginée des demandes de la paroisse ;
- les statistiques ;
- les programmations des 14 prochains jours.

## Seuils du scénario authentifié

- taux d'erreur HTTP inférieur à 1 % ;
- plus de 99 % des contrôles fonctionnels réussis ;
- p95 des lectures métier inférieur à 2 secondes.

Commencer par ce palier de 3 utilisateurs. Toute augmentation doit être décidée
en fonction de la formule Render, des métriques JVM/Hikari/PostgreSQL et des
objectifs de capacité. Ne pas lancer de test d'écriture ou de paiement en
production.
