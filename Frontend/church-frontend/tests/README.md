# Régression de session navigateur

`auth_browser.py` utilise uniquement une API HTTPS et un frontend sur loopback,
avec des comptes jetables issus de `ApplicationStartupSmokeTest`. Ne pas utiliser
une base de production. Il exige Python Playwright, Chromium et OpenSSL.

Préparer une base PostgreSQL jetable, exécuter le smoke test backend avec
`CI_POSTGRES_SMOKE=true`, puis démarrer cette API en HTTPS sur 18443 avec
`JWT_COOKIE_SECURE=true`, `JWT_COOKIE_SAME_SITE=None` et CORS autorisant
`http://127.0.0.1:5173`. Démarrer Vite avec
`VITE_API_BASE_URL=https://localhost:18443` sur 5173.

Depuis la racine : `python3 Frontend/church-frontend/tests/auth_browser.py`.
`AUTH_TEST_FRONTEND`, `AUTH_TEST_API`, `AUTH_TEST_CERT` et `CHROMIUM_PATH`
permettent de changer les chemins locaux. Le certificat public de test est
épinglé par son empreinte SPKI ; les destinations extérieures sont bloquées.

Les scénarios vérifient les cookies tiers restreints, le rechargement, une panne
transitoire, une session non confirmée et deux réponses 401 tardives.

En production, redéployer ensemble backend et frontend : le cookie sécurisé
SameSite=None est partitionné, la connexion est confirmée par `/auth/me` avant
succès et les anciennes requêtes ne peuvent pas effacer une nouvelle session.
Une reconnexion peut être nécessaire après remplacement de l’ancien cookie.
