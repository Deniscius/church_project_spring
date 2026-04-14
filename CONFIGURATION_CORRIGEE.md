# 🔧 CONFIGURATION CORRIGÉE - Rôles & Permissions

## ✅ Architecture Réelle (Basée sur README.md)

### Trois Interfaces Distinctes

```
┌─────────────────────────────────────────┐
│  1. INTERFACE PUBLIQUE (Fidèles)        │
│     ✗ Pas de compte                     │
│     ✓ Endpoints publics uniquement     │
│     ✓ POST /demandes                    │
│     ✓ GET /demandes/code/{code}        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  2. DASHBOARD SECRÉTAIRE                │
│     ✓ Login + Password                  │
│     ✓ Rôle: SECRETAIRE                 │
│     ✓ Gestion des demandes             │
│     ✓ Endpoints protégés                │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  3. DASHBOARD CURÉ + ADMIN              │
│     ✓ Login + Password                  │
│     ✓ Rôles: CURE, ADMIN, SUPER_ADMIN │
│     ✓ Consultation + Validation         │
│     ✓ Endpoints protégés                │
└─────────────────────────────────────────┘
```

---

## 🔐 Rôles Actualisés (SANS USER)

**Seules 4 rôles ont besoin d'être dans la DB:**

```
┌────────────────────────────────────────┐
│       RÔLES AVEC COMPTE (DB)           │
├────────────────────────────────────────┤
│                                        │
│  1. SECRETAIRE (2)                    │
│     └─ Gestion des demandes           │
│                                        │
│  2. CURE (1)                          │
│     └─ Consultation + Validation      │
│                                        │
│  3. ADMIN (3)                         │
│     └─ Gestion paroisse + utilisateurs│
│                                        │
│  4. SUPER_ADMIN (4)                   │
│     └─ Gestion système complet        │
│                                        │
└────────────────────────────────────────┘
```

### Pas de "USER" ou "FIDÈLE" Role

❌ Le rôle "USER" en base de données
✅ Les fidèles accèdent via endpoints publics

---

## 📊 Permissions Corrigées

```
                    │ SECR │ CURE │ ADMIN │ SUPER
────────────────────┼──────┼──────┼───────┼──────
Lire (read)         │  ✓   │  ✓   │   ✓   │  ✓
Créer (create)      │  ✓   │  ✗   │   ✓   │  ✓
Éditer (edit)       │  ✓   │  ✗   │   ✓   │  ✓
Supprimer (delete)  │  ✗   │  ✗   │   ✓   │  ✓
Valider (validate)  │  ✓   │  ✓   │   ✓   │  ✓
Admin               │  ✗   │  ✗   │   ✓   │  ✓
Manage users        │  ✗   │  ✗   │   ✓   │  ✓
System              │  ✗   │  ✗   │   ✗   │  ✓
```

---

## 🔄 Endpoints - Structure Correcte

```
PUBLIC (Pas d'authentification)
├── POST   /demandes                      (Créer demande fidèle)
├── GET    /demandes/code/{code}         (Suivre demande)
├── GET    /paroisses                     (Lister paroisses)
├── GET    /type-demandes                 (Types de messe)
├── GET    /forfait-tarifs                (Tarifs)
├── GET    /horaires                      (Horaires)
└── GET    /facture/code-suivie/{code}   (Invoice publique)

AUTHENTIFIÉS SEULEMENT
├── GET    /demandes                      (Ma liste - filtrée par tenant)
├── GET    /users/{id}                    (Mon profil)
└── ...                                   (Autres ressources)

SECRETAIRE + ADMIN + SUPER_ADMIN
├── PUT    /demandes/{id}                 (Éditer demande)
├── POST   /demandes/{id}/validate        (Valider demande)
└── DELETE /demandes/{id}                 (Supprimer)

ADMIN + SUPER_ADMIN (Gestion utilisateurs)
├── POST   /users                         (Créer user)
├── GET    /users                         (Lister)
├── PUT    /users/{id}                    (Modifier)
└── DELETE /users/{id}                    (Supprimer)

SUPER_ADMIN SEULEMENT (Configuration)
├── POST   /type-demandes                 (Créer type)
├── PUT    /type-demandes/{id}            (Modifier)
├── POST   /forfait-tarifs                (Gérer tarifs)
└── ...                                   (Config système)
```

---

## ✏️ Modifications Nécessaires

### 1. UserRole.java - CORRIGER

```java
public enum UserRole {
    SECRETAIRE,     // Secrétaire de paroisse
    CURE,           // Curé/Prêtre
    ADMIN,          // Admin local
    SUPER_ADMIN     // Super admin
    
    // ❌ PAS DE USER - les fidèles n'ont pas de compte
}
```

### 2. RolePermissions.java - CORRIGER

```java
public class RolePermissions {
    
    static {
        // ✅ SUPER_ADMIN
        ROLE_PERMISSIONS.put(UserRole.SUPER_ADMIN, Set.of(
            PERMISSION_READ,
            PERMISSION_CREATE,
            PERMISSION_EDIT,
            PERMISSION_DELETE,
            PERMISSION_VALIDATE,
            PERMISSION_ADMIN,
            PERMISSION_MANAGE_USERS,
            PERMISSION_MANAGE_SYSTEM
        ));

        // ✅ ADMIN
        ROLE_PERMISSIONS.put(UserRole.ADMIN, Set.of(
            PERMISSION_READ,
            PERMISSION_CREATE,
            PERMISSION_EDIT,
            PERMISSION_DELETE,
            PERMISSION_VALIDATE,
            PERMISSION_MANAGE_USERS
        ));

        // ✅ SECRETAIRE
        ROLE_PERMISSIONS.put(UserRole.SECRETAIRE, Set.of(
            PERMISSION_READ,
            PERMISSION_CREATE,
            PERMISSION_EDIT,
            PERMISSION_VALIDATE
        ));

        // ✅ CURE
        ROLE_PERMISSIONS.put(UserRole.CURE, Set.of(
            PERMISSION_READ,
            PERMISSION_VALIDATE
        ));
        
        // ❌ PAS DE USER
    }
}
```

### 3. SecurityConfig.java - CORRIGER

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // ✅ PUBLIC ENDPOINTS (Pas d'auth)
            .requestMatchers("/auth/login").permitAll()
            .requestMatchers("/auth/logout").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
            
            // ✅ PUBLIC FIDÈLES (Pas d'auth)
            .requestMatchers(HttpMethod.POST, "/demandes").permitAll()
            .requestMatchers(HttpMethod.GET, "/demandes/code/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/paroisses/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/type-demandes/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/forfait-tarifs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/horaires/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/facture/code-suivie/**").permitAll()
            
            // ✅ AUTHENTIFIÉS (Tous les rôles)
            .requestMatchers(HttpMethod.GET, "/demandes").authenticated()
            
            // ✅ SECRETAIRE + ADMIN + SUPER_ADMIN
            .requestMatchers(HttpMethod.PUT, "/demandes/**")
                .hasAnyRole("SECRETAIRE", "ADMIN", "SUPER_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/demandes/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
            
            // ✅ ADMIN + SUPER_ADMIN (Gestion users)
            .requestMatchers(HttpMethod.POST, "/users")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers(HttpMethod.GET, "/users")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers(HttpMethod.PUT, "/users/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/users/**")
                .hasAnyRole("ADMIN", "SUPER_ADMIN")
            
            // ✅ SUPER_ADMIN SEULEMENT (Configuration)
            .requestMatchers(HttpMethod.POST, "/type-demandes")
                .hasRole("SUPER_ADMIN")
            .requestMatchers(HttpMethod.POST, "/forfait-tarifs")
                .hasRole("SUPER_ADMIN")
            
            // ✅ TOUT LE RESTE = authentifié
            .anyRequest().authenticated()
        );
    
    return http.build();
}
```

### 4. Frontend - roles.js CORRIGER

```javascript
// ❌ AVANT
export const ROLES = {
  USER: 'USER',
  SECRETAIRE: 'SECRETAIRE',
  CURE: 'CURE',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

// ✅ APRÈS (Pas de USER)
export const ROLES = {
  SECRETAIRE: 'SECRETAIRE',
  CURE: 'CURE',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

export const ROLE_PERMISSIONS = {
  SUPER_ADMIN: ['read', 'create', 'edit', 'delete', 'validate', 'admin', 'manage_users', 'manage_system'],
  ADMIN: ['read', 'create', 'edit', 'delete', 'validate', 'manage_users'],
  SECRETAIRE: ['read', 'create', 'edit', 'validate'],
  CURE: ['read', 'validate'],
  // ❌ Pas de USER
};
```

---

## 📱 Frontend - Pages Publiques (Fidèles)

```
src/pages/public/
├── DemandesPubliquePage.jsx      (Créer demande - POST /demandes)
├── SuiviDemandePubliquePage.jsx  (Suivre par code - GET /demandes/code/{code})
├── ParoissePubliquePage.jsx      (Sélection paroisse)
└── TypeMessePubliquePage.jsx     (Sélection type de messe)

app/routes/public.routes.jsx
├── POST /demandes              ✓ Accessible sans authentification
├── GET /demandes/code/{code}  ✓ Accessible sans authentification
└── GET /paroisses              ✓ Accessible sans authentification
```

---

## 👤 Frontend - Guards Corrigés

```javascript
// ❌ Pas besoin de guard pour les fidèles
// ✅ Besoin de guards SEULEMENT pour le dashboard

// Routes publiques (Fidèles) = aucun guard
<Route path="/public/demandes" element={<DemandesPubliquePage />} />
<Route path="/public/demandes/suivi" element={<SuiviDemandePubliquePage />} />

// Routes protégées (Backoffice) = guard requis
<Route element={<ProtectedGuard />}>
  <Route element={<RoleGuard allowedRoles={['SECRETAIRE', 'ADMIN']} />}>
    <Route path="/secretaire/dashboard" element={<SecretaireDashboard />} />
  </Route>
  
  <Route element={<RoleGuard allowedRoles={['CURE', 'ADMIN', 'SUPER_ADMIN']} />}>
    <Route path="/cure/dashboard" element={<CureDashboard />} />
  </Route>
  
  <Route element={<RoleGuard allowedRoles={['ADMIN', 'SUPER_ADMIN']} />}>
    <Route path="/admin/dashboard" element={<AdminDashboard />} />
  </Route>
</Route>
```

---

## 🎯 Résumé de la Correction

| Aspect | ❌ Avant | ✅ Après |
|--------|---------|---------|
| Rôles | 5 (USER, CURE, SECRETAIRE, ADMIN, SUPER_ADMIN) | 4 (CURE, SECRETAIRE, ADMIN, SUPER_ADMIN) |
| USER/Fidèle | Role en DB | Public endpoint (pas de compte) |
| Endpoints publics | Accessibles | Toujours accessibles (CORRIGER) |
| Dashboard | Protégé pour tous | Protégé SEULEMENT pour les rôles |
| Architecture | Incohérent | Aligné avec README.md |

---

## 🚀 Prochaines Étapes

1. **Supprimer USER role** - Mettre à jour UserRole.java
2. **Corriger RolePermissions** - Enlever USER 
3. **Corriger SecurityConfig** - Endpoints publics importants
4. **Corriger Frontend roles.js** - Enlever USER et permissions
5. **Tester** - Fidèles n'ont pas besoin de login

**L'architecture doit être:**
- ✅ Fidèles = Accès public SANS compte
- ✅ Secrétaire/Curé/Admin = Accès avec compte + login

Voulez-vous que j'applique ces corrections?
