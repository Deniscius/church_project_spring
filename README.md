# 🌐 ARCHITECTURE COMPLÈTE - VUE D'ENSEMBLE SYSTÈME WEB
## Système de Gestion des Demandes de Messes

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
    │   (Liste des paroisses avec localités)
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
    │     role: "CURE"
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

## 🚀 6. STACK TECHNIQUE WEB

```yaml
Frontend Web (3 applications) :
  Fidèles:
    - Framework: React.js / Vue.js 3
    - UI: Tailwind CSS + Headless UI
    - Forms: React Hook Form + Yup
    - State: Zustand / Pinia
    - Build: Vite
    
  Dashboards (Admin & Secrétaire):
    - Framework: React.js + TypeScript
    - UI: Ant Design / Material-UI
    - Charts: Recharts / Chart.js
    - Tables: TanStack Table
    - State: Redux Toolkit
    - Router: React Router v6

Backend API:
  - Framework: Spring Boot 3.2
  - Java: Java 17 LTS
  - Database: MySQL 8.0+
  - Cache: Redis 7+
  - Security: Spring Security + JWT
  - ORM: Spring Data JPA / Hibernate
  - Migration: Flyway / Liquibase
  - Documentation: Swagger/OpenAPI 3

Infrastructure:
  - Reverse Proxy: Nginx
  - Container: Docker + Docker Compose
  - CI/CD: GitHub Actions / GitLab CI
  - Monitoring: Prometheus + Grafana
  - Logs: ELK Stack (Elasticsearch, Logstash, Kibana)
```

---

## 📦 7. DÉPLOIEMENT

```
┌─────────────────────────────────────────────┐
│           PRODUCTION (Cloud)                 │
└─────────────────────────────────────────────┘

[Domaines]
├─ messes.paroisse.tg         → Frontend Fidèles
├─- admin.messes.paroisse.tg  → Dashboards Admin
└─- api.messes.paroisse.tg    → API Backend

[Serveurs]
├─ Web Server (Nginx)
│  └─ Serve static files + Reverse proxy
│
├─ Application Server
│  ├─ Spring Boot (JAR)
│  └─ JVM tuning (Heap, GC)
│
├─ Database Server
│  ├─ MySQL Primary
│  └─ MySQL Replica (Read)
│
└─ Cache Server
   └─ Redis Cluster

[Services de Notification]
├─ Email: SMTP (SendGrid / MailGun / Gmail SMTP)
│  ├─ Fidèles: Confirmation + Reçu (si email fourni)
│  └─ Admins: Notifications système
│
└─ Paiement Mobile: TMoney/Flooz API
   └─ Intégration webhook pour callbacks

Note: Pas de service SMS (coût élevé)
      Notifications fidèles via Pop-up web

[Sauvegarde]
├─ Database: Backup quotidien (pg_dump)
├─ Files: S3/MinIO
└─ Logs: Rotation 30 jours
```

---

Cette architecture web est **moderne, scalable et sécurisée** ! 🚀

Entities
Utils
Repositiries
DTO 
Mapprrs
service
Controlle
