# 📋 Guide de Configuration des Rôles et Permissions

## Vue d'ensemble

Ce guide explique comment vos rôles et permissions sont configurés côté **Backend** (Spring Boot) et côté **Frontend** (React).

---

## 🔐 Rôles Disponibles

| Rôle | Label | Description | Hiérarchie |
|------|-------|-------------|-----------|
| `USER` | Fidèle | Accès public, peut créer des demandes personnelles | 0 (plus bas) |
| `CURE` | Curé | Consultation et validation des demandes | 1 |
| `SECRETAIRE` | Secrétaire | Gère les demandes et validations de sa paroisse | 2 |
| `ADMIN` | Administrateur | Gestion complète de sa paroisse et ses utilisateurs | 3 |
| `SUPER_ADMIN` | Super Administrateur | Gestion système complète, multi-paroisse | 4 (plus élevé) |

---

## 🛡️ Permissions par Rôle

### SUPER_ADMIN (Super Administrateur)
```
✓ read           - Lire toutes les données
✓ create         - Créer des ressources
✓ edit           - Éditer toutes les ressources
✓ delete         - Supprimer toutes les ressources
✓ validate       - Valider les demandes
✓ admin          - Administrer le système
✓ manage_users   - Gérer les utilisateurs
✓ manage_system  - Accès système complet
```

### ADMIN (Administrateur Local)
```
✓ read           - Lire dans sa paroisse
✓ create         - Créer des ressources
✓ edit           - Éditer les ressources
✓ delete         - Supprimer les ressources
✓ validate       - Valider les demandes
✓ manage_users   - Gérer les utilisateurs de sa paroisse
```

### SECRETAIRE (Secrétaire)
```
✓ read           - Lire les demandes
✓ create         - Créer des demandes
✓ edit           - Éditer les demandes
✓ validate       - Valider les demandes
```

### CURE (Curé)
```
✓ read           - Lire les demandes
✓ validate       - Valider les demandes
```

### USER (Fidèle)
```
✓ read           - Lire ses propres données
✓ create         - Créer ses demandes personnelles
```

---

## 📁 Structure Backend

### 1. **Énumération des Rôles**
📄 `Backend/src/main/java/.../enums/UserRole.java`

```java
public enum UserRole {
    USER,           // Fidèle
    SECRETAIRE,     // Secrétaire
    CURE,           // Curé
    ADMIN,          // Admin local
    SUPER_ADMIN     // Super admin
}
```

### 2. **Gestion des Permissions**
📄 `Backend/src/main/java/.../security/RolePermissions.java`

Classe statique pour vérifier les permissions:
```java
RolePermissions.hasPermission(role, permission);
RolePermissions.canEdit(role);
RolePermissions.isAdmin(role);
```

### 3. **Constantes de Rôles**
📄 `Backend/src/main/java/.../utils/RoleConstants.java`

- Labels des rôles
- Descriptions
- Vérifications hiérarchiques
- Codes d'erreur

### 4. **Contexte Multi-Tenancy**
📄 `Backend/src/main/java/.../security/TenantContext.java`

Gère l'isolation par paroisse:
```java
TenantContext.setTenantId(paroisseId);
TenantContext.getTenantId();
TenantContext.clear();
```

### 5. **Filtre Tenant**
📄 `Backend/src/main/java/.../security/jwt/TenantFilter.java`

Extrait le tenant depuis les headers:
```
X-Tenant-ID: <UUID>
```

### 6. **Configuration de Sécurité**
📄 `Backend/src/main/java/.../security/SecurityConfig.java`

Définit les autorisations par endpoint:
```java
.requestMatchers("/users").hasAnyRole("ADMIN", "SUPER_ADMIN")
.requestMatchers("/demandes").authenticated()
```

---

## 🎨 Structure Frontend

### 1. **Constantes des Rôles**
📄 `Frontend/src/constants/roles.js`

```javascript
export const ROLES = {
  SECRETAIRE: 'SECRETAIRE',
  CURE: 'CURE',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

export const ROLE_LABELS = { /* ... */ };
export const ROLE_PERMISSIONS = { /* ... */ };
export const ROLE_HIERARCHY = { /* ... */ };

// Fonctions utilitaires
export function hasPermission(role, permission) { /* ... */ }
export function canEdit(role) { /* ... */ }
export function isAdmin(role) { /* ... */ }
```

### 2. **Hook usePermissions**
📄 `Frontend/src/hooks/usePermissions.js`

Fournit un accès centralisé aux permissions:
```javascript
const { can, has, isAdminUser, isSuperAdminUser } = usePermissions();

if (can.edit) { /* afficher le bouton edit */ }
if (has('delete')) { /* ... */ }
if (isAdminUser) { /* afficher le dashboard admin */ }
```

### 3. **Guards de Protection de Routes**

#### RoleGuard
📄 `Frontend/src/app/guards/RoleGuard.jsx`

Protège par rôle:
```jsx
<Route element={<RoleGuard allowedRoles={['ADMIN', 'SUPER_ADMIN']} />}>
  <Route path="/admin/dashboard" element={<AdminDashboard />} />
</Route>
```

#### PermissionGuard
📄 `Frontend/src/app/guards/PermissionGuard.jsx`

Protège par permission:
```jsx
<Route element={<PermissionGuard requiredPermissions={['edit', 'delete']} />}>
  <Route path="/admin/edit" element={<EditPage />} />
</Route>
```

### 4. **Composants UI**

#### RoleComponents
📄 `Frontend/src/components/ui/RoleComponents.jsx`

- `RoleSelector` - Sélectionner un rôle
- `RoleBadge` - Afficher un badge de rôle
- `PermissionsList` - Lister les permissions

#### UserProfileCard
📄 `Frontend/src/components/ui/UserProfileCard.jsx`

- `UserProfileCard` - Profil utilisateur complet
- `UserPermissionsInfo` - Afficher ses permissions

### 5. **Page d'Accès Refusé**
📄 `Frontend/src/pages/UnauthorizedPage.jsx`

Affichée quand l'accès est refusé (403).

---

## 🚀 Utilisation

### Backend - Créer un Utilisateur avec un Rôle

```bash
# Requête POST /users
{
  "nom": "Dupont",
  "prenom": "Jean",
  "username": "jean123",
  "password": "SecurePass123!",
  "isGlobal": false,
  "isActive": true,
  "role": "SECRETAIRE"  // ← Spécifier le rôle
}
```

### Backend - Protéger les Endpoints

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public void delete(@PathVariable UUID id) { /* ... */ }

@PreAuthorize("hasAnyRole('SECRETAIRE', 'ADMIN')")
@PostMapping("/demandes")
public Demande create(@RequestBody DemandeRequest req) { /* ... */ }
```

### Frontend - Afficher/Masquer du Contenu

```jsx
import { usePermissions } from '@/hooks/usePermissions';
import { RoleBadge } from '@/components/ui/RoleComponents';

function MyPage() {
  const { can, isAdminUser } = usePermissions();

  return (
    <>
      {isAdminUser && <button>Gérer les utilisateurs</button>}
      
      {can.edit && <button>Éditer</button>}
      
      {can.delete && <button className="bg-red-500">Supprimer</button>}
    </>
  );
}
```

### Frontend - Vérifier Avant une Action

```javascript
import { usePermissions } from '@/hooks/usePermissions';

function DemandeForm() {
  const { has, can } = usePermissions();

  const handleDelete = async () => {
    if (!can.delete) {
      alert('Vous n\'avez pas les permissions pour supprimer');
      return;
    }
    // Procéder à la suppression...
  };

  return (
    <button onClick={handleDelete} disabled={!can.delete}>
      Supprimer
    </button>
  );
}
```

---

## 🔒 Multi-Tenancy (Isolation par Paroisse)

### Backend - Utiliser le TenantContext

```java
@Service
public class DemandeService {
    
    public List<Demande> getDemandes() {
        UUID tenantId = TenantContext.getTenantId();
        // Charger uniquement les demandes de cette paroisse
        return repository.findByParoisseId(tenantId);
    }
}
```

### Frontend - Envoyer le Tenant ID

```javascript
// Ajouter dans les headers
const headers = {
  'X-Tenant-ID': currentParishId,  // UUID de la paroisse
  'Authorization': `Bearer ${token}`,
};

await apiClient('/demandes', {
  method: 'GET',
  headers,
});
```

---

## 📊 Flux d'Authentification

```
┌─────────────────────────────────────────┐
│ 1. LOGIN (Frontend)                     │
│   POST /auth/login                      │
│   { username, password }                │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 2. AUTHENTICATION (Backend)             │
│   AuthService.login()                   │
│   → Vérifier credentials                │
│   → Charger l'utilisateur et son rôle   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 3. GENERATE JWT (Backend)               │
│   JwtUtils.generateToken()              │
│   → Inclure le rôle dans les claims     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 4. JWT RESPONSE (Backend)               │
│   JwtResponse {                         │
│     accessToken,                        │
│     fullName,                           │
│     roles: ["ROLE_ADMIN"],              │
│     ...                                 │
│   }                                     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 5. STORE SESSION (Frontend)             │
│   mapJwtToUser()                        │
│   → Transformer en objet user           │
│   → Stocker dans sessionStorage         │
│   → Mettre à jour AuthContext           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 6. USE PERMISSIONS (Frontend)           │
│   usePermissions()                      │
│   → Vérifier les droits avant action    │
│   → Afficher/masquer l'UI               │
│   → RoleGuard/PermissionGuard           │
└─────────────────────────────────────────┘
```

---

## 🛠️ Configuration & Maintenance

### Backend - Ajouter un Nouveau Rôle

1. Ajouter l'enum dans `UserRole.java`:
   ```java
   public enum UserRole {
       // ...
       NEW_ROLE
   }
   ```

2. Ajouter les permissions dans `RolePermissions.java`:
   ```java
   ROLE_PERMISSIONS.put(UserRole.NEW_ROLE, Set.of(
       PERMISSION_READ,
       PERMISSION_CREATE
   ));
   ```

3. Ajouter les labels dans `RoleConstants.java`:
   ```java
   ROLE_LABELS.put(UserRole.NEW_ROLE, "Nouveau Rôle");
   ```

### Frontend - Ajouter un Nouveau Rôle

1. Ajouter dans `roles.js`:
   ```javascript
   export const ROLES = {
       NEW_ROLE: 'NEW_ROLE',
   };
   
   export const ROLE_LABELS = {
       NEW_ROLE: 'Nouveau Rôle',
   };
   ```

2. Mettre à jour `usePermissions.js`:
   ```javascript
   isNewRole: userRole === ROLES.NEW_ROLE,
   ```

---

## ⚠️ Points Importants

### ✅ À FAIRE

- ✓ Toujours vérifier les permissions au backend (ne pas faire confiance au frontend)
- ✓ Utiliser les Guards pour protéger les routes du frontend
- ✓ Utiliser `usePermissions()` pour afficher/masquer l'UI
- ✓ Stocker le JWT de manière sécurisée
- ✓ Inclure le tenant ID dans les requêtes (multi-tenancy)
- ✓ Logger les accès refusés pour audit

### ❌ À ÉVITER

- ✗ Ne pas stocker les mots de passe en clair
- ✗ Ne pas faire confiance aux permissions du frontend uniquement
- ✗ Ne pas exposer d'informations sensibles dans les réponses
- ✗ Ne pas oublier de nettoyer le `TenantContext` après utilisation
- ✗ Ne pas accepter les demandes sans token JWT (sauf endpoints publics)

---

## 📞 Aide et Support

Pour toute question ou problème de configuration:

1. Vérifier les logs du backend pour les erreurs d'authentification
2. Vérifier que le JWT contient les bons rôles
3. Vérifier que les permissions du frontend matchent le backend
4. Vérifier que le tenant ID est correctement propagé

---

**Version**: 1.0.0  
**Dernière mise à jour**: 2026-04-14
