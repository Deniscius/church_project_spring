# 🌐 ARCHITECTURE COMPLÈTE - VUE D'ENSEMBLE SYSTÈME WEB
## Système de Gestion des Demandes de Messes

## Règles métier actuellement implémentées

- Un **doyenné** regroupe plusieurs paroisses ; chaque paroisse appartient à un doyenné.
- Chaque type de demande définit un délai minimum, en heures, avant la première célébration (24 h par défaut).
- Le backend contrôle ensemble la date, l'heure et le fuseau `Africa/Lome` : une célébration passée ou trop proche est refusée.
- Le paiement **au comptant** correspond au mode `ESPECES` et s'effectue dans la paroisse sélectionnée.
- Après le dépôt, le fidèle peut télécharger un reçu PDF via `GET /demandes/code/{codeSuivie}/recu.pdf`.

---

## 📊 1. ARCHITECTURE GLOBALE DU SYSTÈME

```
┌─────────────────────────────────────────────────────────────────────┐
│                         UTILISATEURS FINAUX                          │
└─────────────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   FIDÈLES     │   │  SECRÉTAIRE   │   │     CURÉ      │
│   (Web App)   │   │  (Dashboard)  │   │  (Dashboard)  │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │         REVERSE PROXY (Nginx)         │
        │    • Load Balancing                   │
        │    • SSL/TLS Termination              │
        │    • Rate Limiting                    │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │      API REST (Spring Boot)           │
        │    • Authentication JWT               │
        │    • Business Logic                   │
        │    • Data Validation                  │
        └───────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  PostgreSQL   │   │  Redis Cache  │   │   Services    │
│   (Database)  │   │  (Sessions)   │   │   Externes    │
└───────────────┘   └───────────────┘   └───────────────┘
                                                │
                                    ┌───────────┼───────────┐
                                    │           │           │
                                    ▼           ▼           ▼
                            ┌──────────┐ ┌──────────┐ ┌──────────┐
                            │  Email   │ │  TMoney  │ │  Flooz   │
                            │  SMTP    │ │  /Flooz  │ │   API    │
                            └──────────┘ └──────────┘ └──────────┘
                            
Note: SMS désactivé pour les fidèles (coût élevé)
      Notifications par Pop-up + Email uniquement
```

---

## 🎯 2. TROIS INTERFACES WEB DISTINCTES

### **A. Interface Publique Fidèles (Web Responsive)**

```
┌────────────────────────────────────────────────────────────┐
│                    SITE PUBLIC FIDÈLES                      │
│                    https://messes.paroisse.tg               │
└────────────────────────────────────────────────────────────┘

📱 PAGES PRINCIPALES :
├── 🏠 Accueil
│   ├── Présentation du système
│   ├── [Faire une demande de messe]
│   └── [Suivre ma demande]
│
├── 📋 Nouvelle Demande
│   ├── Étape 1 : Sélection paroisse
│   ├── Étape 2 : Type de messe
│   ├── Étape 3 : Dates et horaire
│   ├── Étape 4 : Informations fidèle
│   ├── Étape 5 : Récapitulatif
│   └── Étape 6 : Paiement en ligne
│
├── 🔍 Suivi de Demande
│   ├── Recherche par code suivi
│   ├── Détails de la demande
│   └── Statut en temps réel
│
└── 📞 Contact
    └── Informations paroisses

🎨 CARACTÉRISTIQUES :
✅ Design moderne et responsive
✅ Processus en étapes (stepper)
✅ Paiement intégré (TMoney/Flooz)
✅ Pas de compte requis
✅ Multilingue (Français/Éwé)
```

### **B. Dashboard Secrétaire (Backoffice)**

```
┌────────────────────────────────────────────────────────────┐
│                  DASHBOARD SECRÉTAIRE                       │
│                  https://admin.messes.paroisse.tg           │
└────────────────────────────────────────────────────────────┘

🔐 ACCÈS : Login + Password

📊 VUE D'ENSEMBLE :
├── 📈 Dashboard
│   ├── Statistiques du jour
│   ├── Demandes récentes
│   └── Revenus du jour
│
├── ➕ Enregistrement Sur Place
│   ├── Formulaire simplifié
│   ├── Saisie informations fidèle
│   ├── Réception paiement espèces
│   └── Impression reçu instantané
│
├── 📋 Gestion Demandes
│   ├── Liste toutes demandes
│   ├── Filtres (date, statut)
│   ├── Recherche rapide
│   └── Export Excel/PDF
│
├── 🎨 Types de Demande
│   ├── Créer nouveau type
│   ├── Modifier type existant
│   ├── Gérer forfaits
│   └── Activer/Désactiver
│
├── 🕐 Horaires
│   ├── Horaires hebdomadaires
│   └── Horaires spéciaux
│
└── 👤 Mon Compte
    └── Profil + Paramètres

🎯 FONCTIONNALITÉS CLÉS :
✅ Interface simple et rapide
✅ Impression directe
✅ Hors ligne (Progressive Web App)
✅ Notifications navigateur
```

### **C. Dashboard Curé (Administration)**

```
┌────────────────────────────────────────────────────────────┐
│                    DASHBOARD CURÉ                           │
│                    https://admin.messes.paroisse.tg         │
└────────────────────────────────────────────────────────────┘

🔐 ACCÈS : Login + Password + 2FA (optionnel)

📊 VUE D'ENSEMBLE :
├── 📈 Dashboard Avancé
│   ├── Métriques globales
│   ├── Graphiques revenus
│   ├── Tendances mensuelles
│   └── Prévisions
│
├── ⚠️ Validation Messes Spéciales
│   ├── File d'attente
│   ├── Détails demande
│   ├── Contact fidèle (clic-to-call)
│   ├── Historique contact
│   ├── Notes privées
│   ├── Notes publiques (fidèle)
│   └── [Valider] [Refuser]
│
├── 📋 Gestion Demandes (Vue Complète)
│   ├── Toutes les demandes
│   ├── Filtres avancés
│   ├── Édition manuelle
│   └── Historique complet
│
├── 📊 Statistiques & Rapports
│   ├── Revenus par période
│   ├── Types de messes populaires
│   ├── Taux de validation
│   ├── Fidèles récurrents
│   └── Export comptable
│
├── 👥 Gestion Équipe
│   ├── Liste admins
│   ├── Créer secrétaire/gérant
│   ├── Permissions
│   └── Historique activités
│
├── ⚙️ Configuration Paroisse
│   ├── Informations générales
│   ├── Types de demande
│   ├── Forfaits tarifaires
│   └── Horaires
│
└── 📧 Communication
    ├── Envoyer SMS groupé
    └── Modèles de messages

🎯 FONCTIONNALITÉS CLÉS :
✅ Tableau de bord analytique
✅ Workflow validation optimisé
✅ Rapports exportables
✅ Gestion d'équipe complète
✅ Audit trail complet
```

---

## 🔄 3. FLUX DE DONNÉES PRINCIPAUX

### **Flux 1 : Création Demande par Fidèle (Web)**

```
┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 1 : SÉLECTION PAROISSE                                │
└─────────────────────────────────────────────────────────────┘
Fidèle (Navigateur)
    │
    ├─> GET /api/paroisses
    │   (Liste des paroisses avec leur doyenné)
    │
    └─> Sélection : Cathédrale Sacré-Cœur

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 2 : CHOIX TYPE DE MESSE                              │
└─────────────────────────────────────────────────────────────┘
    │
    ├─> GET /api/paroisses/{id}/types-demande
    │   (Types disponibles : normale/dominicale/spéciale)
    │
    └─> Sélection : Messe spéciale

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 3 : DATES ET HORAIRE                                 │
└─────────────────────────────────────────────────────────────┘
    │
    ├─> GET /api/paroisses/{id}/horaires
    │   (Horaires disponibles par jour)
    │
    ├─> Sélection dates : 15/12/2024
    ├─> Choix horaire : Personnalisé 15h30
    │
    └─> POST /api/calcul-tarif
        Request: {
          type_demande_id,
          date_debut,
          date_fin,
          horaire_personnalisee
        }
        Response: {
          montant: 5000,
          details_calcul: {...}
        }

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 4 : INFORMATIONS FIDÈLE                              │
└─────────────────────────────────────────────────────────────┘
    │
    └─> Saisie :
        - Nom : KOFI
        - Prénom : Amavi
        - Téléphone : +228 90 12 34 56
        - Email : kofi@example.com (OPTIONNEL)
        - Intention : "Anniversaire de mariage"

┌─────────────────────────────────────────────────────────────┐
│  ÉTAPE 5 : CONFIRMATION & PAIEMENT                          │
└─────────────────────────────────────────────────────────────┘
    │
    ├─> POST /api/demandes
    │   Request: {
    │     id_paroisse,
    │     id_type_demande,
    │     fidele: {...},
    │     dates: {...},
    │     intention
    │   }
    │   Response: {
    │     demande_id,
    │     code_suivie: "DEM20241203ABCD",
    │     montant: 5000,
    │     statut: "en_attente_paiement"
    │   }
    │
    ├─> POST /api/paiement/initier
    │   Request: {
    │     demande_id,
    │     mode: "tmoney",
    │     telephone: "+22890123456",
    │     montant: 5000
    │   }
    │
    │   API Backend ─────> TMoney API
    │                      (Initier transaction)
    │   
    │   TMoney ──────────> Notification téléphone Fidèle
    │                      "Confirmez paiement avec PIN"
    │
    │   Fidèle ──────────> Confirme sur téléphone
    │
    │   TMoney ──────────> Callback API
    │                      /api/paiement/callback
    │
    ├─> Mise à jour automatique :
    │   - statut_paiement = "paye"
    │   - Création facture
    │   - Enregistrement details_paiement
    │   - Envoi email au fidèle (SI email fourni)
    │   - Envoi email notification à la paroisse
    │
    └─> 🔔 NOTIFICATION POP-UP (sur la page web)
        ┌──────────────────────────────────────────┐
        │  ✅ DEMANDE CRÉÉE AVEC SUCCÈS !          │
        │                                          │
        │  📋 Code de suivi : DEM20241203ABCD      │
        │  💰 Montant : 5 000 FCFA                 │
        │  ✅ Paiement : Confirmé                   │
        │                                          │
        │  ⚠️ IMPORTANT : Notez votre code de suivi│
        │  Vous en aurez besoin pour consulter    │
        │  votre demande.                          │
        │                                          │
        │  📧 Un email de confirmation a été envoyé│
        │  (si vous avez fourni votre email)      │
        │                                          │
        │  [📄 Télécharger le reçu PDF]            │
        │  [🔍 Suivre ma demande]                  │
        │  [✕ Fermer]                              │
        └──────────────────────────────────────────┘
        
        + Option : Copier le code dans le presse-papier
        + Option : Envoyer par email (si fourni)
        + Option : Imprimer le reçu
```

### **Flux 2 : Validation par Curé (Dashboard)**

```
┌─────────────────────────────────────────────────────────────┐
│  CURÉ SE CONNECTE                                            │
└─────────────────────────────────────────────────────────────┘
Curé (Dashboard)
    │
    ├─> POST /api/auth/login
    │   Request: {
    │     username: "pere.mensah",
    │     password: "***"
    │   }
    │   Response: {
    │     access_token: "eyJ...",
    │     refresh_token: "...",
    │     user: {...},
    │     user: "CURE"
    │   }
    │
    └─> Redirection : /dashboard

┌─────────────────────────────────────────────────────────────┐
│  DASHBOARD CHARGÉ                                           │
└─────────────────────────────────────────────────────────────┘
    │
    ├─> GET /api/admin/dashboard
    │   Header: Authorization: Bearer {token}
    │   Response: {
    │     stats: {
    │       demandes_en_attente: 3,
    │       validees_mois: 142,
    │       revenus_mois: 340000
    │     },
    │     demandes_urgentes: [...]
    │   }
    │
    └─> Badge notification : "3 demandes à valider"

┌─────────────────────────────────────────────────────────────┐
│  VALIDATION D'UNE DEMANDE                                   │
└─────────────────────────────────────────────────────────────┘
    │
    ├─> GET /api/admin/demandes?statut=en_attente
    │   Response: {
    │     demandes: [
    │       {
    │         id: "...",
    │         code_suivie: "DEM20241203ABCD",
    │         fidele: {
    │           nom: "KOFI",
    │           prenom: "Amavi",
    │           tel: "+22890123456",
    │           email: "kofi@example.com"
    │         },
    │         type_messe: "Messe spéciale",
    │         date: "15/12/2024 15h30",
    │         intention: "Anniversaire de mariage",
    │         montant: 5000,
    │         paye: true
    │       }
    │     ]
    │   }
    │
    ├─> Curé clique sur demande
    │   Modal s'ouvre avec détails complets
    │
    ├─> Curé appelle le fidèle
    │   Clic-to-call : +22890123456
    │
    ├─> PUT /api/admin/demandes/{id}/contact
    │   Request: {
    │     contact_effectue: true,
    │     date_contact: "2024-12-04T10:30:00"
    │   }
    │
    ├─> Curé valide après vérification
    │
    └─> PUT /api/admin/demandes/{id}/valider
        Request: {
          notes_admin: "Confirmé avec le fidèle",
          notes_publiques: "Votre messe est confirmée..."
        }
        
        Backend traite :
        ├─> UPDATE demande SET statut_validation='validee'
        ├─> INSERT audit_log (traçabilité)
        ├─> Envoyer EMAIL au fidèle (si email fourni)
        │   Objet: "Demande DEM20241203ABCD validée"
        │   Contenu: Confirmation + Détails + Notes publiques
        │
        └─> Envoyer EMAIL au curé
            Objet: "Confirmation de validation"
            Contenu: Récapitulatif de l'action
        
        Response: {
          success: true,
          message: "Demande validée"
        }
    
    ├─> 🔔 NOTIFICATION POP-UP (Dashboard curé)
    │   "Demande DEM20241203ABCD validée avec succès"
    │
    └─> Mise à jour automatique de la liste
        Demande disparaît de "En attente"
```

### **Flux 3 : Enregistrement Sur Place (Secrétaire)**

```
┌─────────────────────────────────────────────────────────────┐
│  FIDÈLE SE PRÉSENTE À LA PAROISSE                           │
└─────────────────────────────────────────────────────────────┘
Secrétaire (Dashboard)
    │
    ├─> Clic : [Nouvelle demande sur place]
    │
    ├─> Formulaire simplifié s'affiche
    │   - Informations fidèle (nom, prénom, tél)
    │   - Type de messe
    │   - Dates
    │   - Intention
    │
    ├─> Calcul automatique montant
    │
    ├─> Fidèle paie en espèces
    │
    └─> POST /api/admin/demandes/sur-place
        Request: {
          paroisse_id: "...", (auto depuis session)
          fidele: {...},
          type_demande_id,
          dates: {...},
          intention,
          paiement: {
            type: "especes",
            montant: 5000,
            reference: "ESP-20241203-001"
          }
        }
        
        Backend traite :
        ├─> INSERT demande
        ├─> INSERT facture
        ├─> INSERT details_paiement
        ├─> Génération code_suivie
        │
        ├─> Si email fidèle fourni:
        │   └─> Envoyer EMAIL récapitulatif
        │
        └─> Envoyer EMAIL à la paroisse
            Objet: "Nouvelle demande enregistrée"
            Destinataire: contact@paroisse.tg
        
        Response: {
          success: true,
          code_suivie: "DEM20241203WXYZ",
          recu_url: "/recu/download/..."
        }
    
    ├─> 🔔 NOTIFICATION POP-UP (Dashboard secrétaire)
    │   ┌──────────────────────────────────────────┐
    │   │  ✅ DEMANDE ENREGISTRÉE !                │
    │   │                                          │
    │   │  📋 Code : DEM20241203WXYZ               │
    │   │  💰 Montant : 5 000 FCFA                 │
    │   │  ✅ Paiement : Espèces reçues            │
    │   │                                          │
    │   │  [📄 Imprimer le reçu]                   │
    │   │  [✕ Fermer]                              │
    │   └──────────────────────────────────────────┘
    │
    └─> [Imprimer reçu] (automatique)
        Impression directe du reçu
```

---

## 🗄️ 4. ARCHITECTURE DE DONNÉES

### **Modèle de Données Simplifié**

```
┌────────────────┐
│  TYPE_DEMANDE  │ 1
└────────────────┘
        │
        │ N
        ▼
┌────────────────┐         ┌──────────────┐
│    DEMANDE     │ N ────→ 1│   PAROISSE   │
└────────────────┘         └──────────────┘
        │                          │
        │ 1                        │ N
        ▼                          ▼
┌────────────────┐         ┌──────────────┐
│    FACTURE     │         │   HORAIRE    │
└────────────────┘         └──────────────┘
        │
        │ 1
        ▼
┌────────────────┐
│DETAILS_PAIEMENT│
└────────────────┘

CLÉS :
• demande.fidele_tel = IDENTIFIANT UNIQUE du fidèle
• demande.code_suivie = Recherche publique
• admin_paroisse.id_paroisse = Scope des données
```

---

## 🔐 5. SÉCURITÉ & AUTHENTIFICATION

### **Trois Niveaux de Sécurité**

```
┌────────────────────────────────────────────────────┐
│  NIVEAU 1 : FIDÈLES (Public)                       │
│  • Pas d'authentification                          │
│  • Recherche par code_suivie uniquement            │
│  • Rate limiting : 10 req/min par IP               │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│  NIVEAU 2 : SECRÉTAIRE (Backoffice)                │
│  • Login + Password                                │
│  • JWT Token (expire 8h)                           │
│  • Scope : SA paroisse uniquement                  │
│  • Permissions limitées                            │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│  NIVEAU 3 : CURÉ (Administration)                  │
│  • Login + Password                                │
│  • JWT Token (expire 4h)                           │
│  • 2FA optionnel (SMS)                             │
│  • Scope : SA paroisse + Validation                │
│  • Audit log complet                               │
└────────────────────────────────────────────────────┘
```

---

## 🚀 6. STACK TECHNIQUE (actuel)

```yaml
Frontend (une app Vite) :
  - React 19 + React Router 7
  - TanStack Query
  - Build: Vite 8
  - Auth navigateur: cookie HttpOnly (pas de JWT en localStorage)

Backend API:
  - Spring Boot 3.5 / Java 17
  - PostgreSQL + Flyway
  - Spring Security + JWT cookie (AES-GCM optionnel)
  - Multi-tenant Hibernate filter
  - Paiements: FedaPay (webhook HMAC)

Infrastructure:
  - Render: Postgres + API Docker + static site (render.yaml)
  - CI: GitHub Actions (.github/workflows/ci.yml)
  - Dependabot: Maven + npm
  - Docs ops: docs/RENDER.md, docs/FEDAPAY.md, docs/SCALING.md
```

---

## 📦 7. DÉPLOIEMENT

**Cible actuelle : Render** — procédure dans [`docs/RENDER.md`](docs/RENDER.md), blueprint [`render.yaml`](render.yaml).

```
[Render]
├─ missanye-web   → Static Vite (CSP + rewrite SPA)
├─ missanye-api   → Docker Spring Boot (health /actuator/health)
└─ missanye-db    → PostgreSQL managé + disque uploads API

[Sécurité prod]
├─ SPRING_PROFILES_ACTIVE=prod
├─ JWT cookie Secure + SameSite=Lax
├─ SMTP obligatoire (app.mail.fail-closed)
├─ CORS = URL front exacte
└─ Webhook FedaPay → /webhooks/fedapay
```

