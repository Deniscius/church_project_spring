# ✅ CHECKLIST FINALE - INTÉGRATION RÔLES & PERMISSIONS

## 🎯 Avant de Déployer

Utilisez cette checklist pour vérifier que tout est correctement intégré.

---

## 📦 BACKEND - Fichiers Ajoutés/Modifiés

### Nouveaux Fichiers
- [ ] ✅ `Backend/.../security/RolePermissions.java` - Gestion des permissions
- [ ] ✅ `Backend/.../security/TenantContext.java` - Contexte tenant
- [ ] ✅ `Backend/.../security/jwt/TenantFilter.java` - Filtre tenant
- [ ] ✅ `Backend/.../utils/RoleConstants.java` - Constantes et labels

### Fichiers Modifiés
- [ ] ✅ `Backend/.../enums/UserRole.java` - Ajouté 3 nouveaux rôles
- [ ] ✅ `Backend/.../security/SecurityConfig.java` - Amélioré configuration

---

## 🎨 FRONTEND - Fichiers Ajoutés/Modifiés

### Nouveaux Fichiers
- [ ] ✅ `Frontend/.../guards/PermissionGuard.jsx` - Permission granulaire
- [ ] ✅ `Frontend/.../components/ui/RoleComponents.jsx` - 3 composants UI
- [ ] ✅ `Frontend/.../components/ui/UserProfileCard.jsx` - 2 composants profil
- [ ] ✅ `Frontend/.../pages/UnauthorizedPage.jsx` - Page 403

### Fichiers Modifiés
- [ ] ✅ `Frontend/.../constants/roles.js` - Rôles, permissions, fonctions utilitaires
- [ ] ✅ `Frontend/.../hooks/usePermissions.js` - Hook amélioré
- [ ] ✅ `Frontend/.../app/guards/RoleGuard.jsx` - Amélioré guard

---

## 📚 Documentation Ajoutée

- [ ] ✅ `ROLES_PERMISSIONS_GUIDE.md` - Guide complet
- [ ] ✅ `ROLES_PERMISSIONS_EXAMPLES.js` - Tous les exemples de code
- [ ] ✅ `IMPLEMENTATION_SUMMARY.md` - Résumé des modifications
- [ ] ✅ `TENANT_FILTER_INTEGRATION.md` - Intégration du TenantFilter
- [ ] ✅ `CHECKLIST_FINAL.md` - Cette checklist

---

## 🔐 Vérifications Backend

### 1. Énumération des Rôles
```java
// ✓ Vérifier que UserRole.java contient :
USER       // ✓
SECRETAIRE // ✓ (Nouveau)
CURE       // ✓ (Nouveau)
ADMIN      // ✓
SUPER_ADMIN // ✓ (Nouveau)
```

- [ ] 5 rôles présents
- [ ] Commentaires de hiérarchie présents

### 2. Permissions
```java
// ✓ Vérifier que RolePermissions.java contient :
- hasPermission(role, permission)
- canRead(role)
- canCreate(role)
- canEdit(role)
- canDelete(role)
- canValidate(role)
- isAdmin(role)
- isSuperAdmin(role)
- isAdminOrSuperAdmin(role)
```

- [ ] Classe RolePermissions existe
- [ ] 8+ méthodes publiques
- [ ] Map statique ROLE_PERMISSIONS avec 5 rôles
- [ ] Permissions cohérentes avec le tableau des permissions

### 3. Sécurité - Configuration
```java
// ✓ Vérifier SecurityConfig.java :
- @EnableMethodSecurity (ou @EnableGlobalMethodSecurity)
- RequestMatchers protégés avec hasRole()
- ADMIN/SUPER_ADMIN sur /users
- SECRETAIRE/ADMIN/SUPER_ADMIN sur /demandes PUT
- SUPER_ADMIN sur configuration système
```

- [ ] SecurityConfig importe TenantFilter
- [ ] TenantFilter ajouté à la chaîne
- [ ] Endpoints publics explicitement permitAll()
- [ ] Endpoints authentifiés explicitement .authenticated()
- [ ] Endpoints admin explicitement hasRole()

### 4. JWT & Authentication
```java
// ✓ Vérifier que JwtResponse inclut :
- accessToken
- roles (ex: ["ROLE_ADMIN"])
- fullName
- username
- publicId
```

- [ ] Classe JwtResponse existe
- [ ] Méthode `from()` statique présente
- [ ] Roles parsés correctement depuis authorities

### 5. UserDetailsImpl
```java
// ✓ Vérifier que UserDetailsImpl :
- Implémente UserDetails
- Contient les rôles avec préfixe "ROLE_"
- Méthode build(User) statique
```

- [ ] Classe UserDetailsImpl existe
- [ ] Méthode build() crée SimpleGrantedAuthority("ROLE_" + role)
- [ ] enabled = isActive (du User)

---

## 🎨 Vérifications Frontend

### 1. Constantes des Rôles
```javascript
// ✓ Vérifier que roles.js contient :
export const ROLES = { 
  USER, CURE, SECRETAIRE, ADMIN, SUPER_ADMIN 
}
export const ROLE_LABELS = { /* 5 rôles */ }
export const ROLE_DESCRIPTIONS = { /* 5 rôles */ }
export const ROLE_PERMISSIONS = { /* 5 rôles */ }
export const ROLE_HIERARCHY = { /* 5 rôles */ }
```

- [ ] ROLES enum avec 5 rôles
- [ ] ROLE_LABELS avec 5 labels français
- [ ] ROLE_DESCRIPTIONS avec descriptions détaillées
- [ ] ROLE_PERMISSIONS = permissions par rôle
- [ ] ROLE_HIERARCHY = niveaux numériques
- [ ] Fonctions utilitaires: hasPermission, canRead, canEdit, canDelete, canValidate, isAdmin, isSuperAdmin, isHierarchyGreaterOrEqual

### 2. Hook usePermissions
```javascript
// ✓ Vérifier que usePermissions retourne :
{
  has(permission),
  can: { read, create, edit, delete, validate },
  role,
  isAdminUser,
  isSuperAdminUser,
  isSecretaire,
  isCure,
  isUser,
  isHierarchyGreaterOrEqual(requiredRole),
  hasAnyRole(...roles),
  hasAllRoles(...roles),
  allPermissions,
  check(condition)
}
```

- [ ] Hook usePermissions amélioré
- [ ] Retourne object avec 10+ propriétés
- [ ] Callback `has()` fonctionne
- [ ] Callback `can.*` fonctionnent
- [ ] Vérifications de rôle exists
- [ ] Hiérarchie vérifiable

### 3. RoleGuard Amélioré
```jsx
// ✓ Vérifier que RoleGuard.jsx :
- Accepte allowedRoles (array)
- Accepte fallbackPath (string)
- Accepte requireAll (boolean)
- Redirige vers /unauthorized par défaut
- Affiche un warning console
```

- [ ] RoleGuard importe useAuth et usePermissions
- [ ] Vérifie isAuthenticated d'abord
- [ ] Support allowedRoles array
- [ ] Support requireAll booléen
- [ ] Fallback vers /unauthorized
- [ ] Console.warn pour accès refusés

### 4. PermissionGuard (Nouveau)
```jsx
// ✓ nouveau fichier PermissionGuard.jsx :
- Accepte requiredPermissions (array)
- Accepte fallbackPath (string)
- Accepte requireAll (boolean)
```

- [ ] Fichier PermissionGuard existe
- [ ] Accepte requiredPermissions
- [ ] Support requireAll
- [ ] Fallback configurable

### 5. Composants UI
```jsx
// ✓ Vérifier RoleComponents.jsx contient :
- RoleSelector
- RoleBadge
- PermissionsList
```

- [ ] Importable depuis ui/RoleComponents
- [ ] RoleSelector avec options
- [ ] RoleBadge avec couleurs
- [ ] PermissionsList avec symboles

### 6. UserProfileCard
```jsx
// ✓ Vérifier UserProfileCard.jsx contient :
- UserProfileCard
- UserPermissionsInfo
```

- [ ] Composants créés
- [ ] Affiche UUID utilisateur
- [ ] Affiche rôle avec badge
- [ ] Affiche permissions avec checkboxes

### 7. UnauthorizedPage
```jsx
// ✓ Vérifier UnauthorizedPage.jsx :
- Affiche 403 Forbidden
- Bouton retour
- Bouton accueil (redirige selon rôle)
```

- [ ] Page existe et est accessible
- [ ] Affiche message d'erreur 403
- [ ] Montre le rôle actuel
- [ ] Boutons de navigation présents

---

## 🧪 Tests Fonctionnels

### Backend Tests

#### 1. Création d'utilisateurs avec rôles
```bash
# ✓ Tester chaque rôle
curl -X POST http://localhost:8081/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Test",
    "prenom": "Role",
    "username": "test_role",
    "password": "Test123!",
    "role": "SECRETAIRE",
    "isGlobal": false,
    "isActive": true
  }'
```

- [ ] USER crée sans erreurs
- [ ] SECRETAIRE crée sans erreurs
- [ ] CURE crée sans erreurs
- [ ] ADMIN crée sans erreurs
- [ ] SUPER_ADMIN crée sans erreurs

#### 2. Login avec chaque rôle
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_role",
    "password": "Test123!"
  }'
```

- [ ] Retourne JWT valide
- [ ] JWT contient le rôle correct
- [ ] JWT contient les permissions correctes
- [ ] fullName parsé correctement

#### 3. Accès aux endpoints protégés
```bash
# ✓ ADMIN peut créer utilisateurs
curl -X POST http://localhost:8081/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# ✗ USER ne peut pas créer utilisateurs
curl -X POST http://localhost:8081/users \
  -H "Authorization: Bearer $USER_TOKEN"
```

- [ ] ADMIN peut POST /users (201)
- [ ] USER ne peut pas POST /users (403)
- [ ] SECRETAIRE peut PUT /demandes (200)
- [ ] USER ne peut pas PUT /demandes (403)
- [ ] CURE peut POST /demandes/{id}/validate (200)
- [ ] USER ne peut pas POST /demandes/{id}/validate (403)

#### 4. Permissions vérifiées côté serveur
```java
// ✓ Logs doivent montrer :
RolePermissions.isAdmin(ADMIN) = true
RolePermissions.isAdmin(USER) = false
RolePermissions.canDelete(ADMIN) = true
RolePermissions.canDelete(USER) = false
```

- [ ] RolePermissions retourne les bons booléens
- [ ] Cohérence entre frontend et backend

### Frontend Tests

#### 1. usePermissions hook
```javascript
// ✓ Dans la console du navigateur
const { can, isAdminUser } = require('@/hooks/usePermissions');
console.log(can.edit);       // true ou false
console.log(isAdminUser);     // true ou false
console.log(allPermissions);  // ['read', 'create', ...]
```

- [ ] Hook s'exporte correctement
- [ ] Retourne object avec propriétés attendues
- [ ] Valeurs cohérentes avec le rôle

#### 2. RoleGuard fonctionne
```jsx
// ✓ Testet accès à /admin/dashboard
// - Avec rôle ADMIN : affiche page
// - Avec rôle USER : redirige /unauthorized
```

- [ ] ADMIN ➜ Access granted
- [ ] USER ➜ Redirected to /unauthorized
- [ ] NON AUTHENTIFIÉ ➜ Redirected to /login

#### 3. Boutons affichés/masqués correctement
```jsx
// ✓ Tester avec différents rôles
// ADMIN :
  - Edit button ✓
  - Delete button ✓
  - Admin panel ✓
  
// USER :
  - Edit button ✗
  - Delete button ✗
  - Admin panel ✗
```

- [ ] Boutons edit visibles pour ADMIN
- [ ] Boutons edit invisibles pour USER
- [ ] Boutons delete visibles pour ADMIN
- [ ] Admin panel visible pour ADMIN/SUPER_ADMIN

#### 4. Page /unauthorized accessible
```
GET http://localhost:3000/unauthorized
```

- [ ] Page charge sans erreurs
- [ ] Affiche message 403
- [ ] Boutons de navigation fonctionnent

---

## 🔌 Intégration - Checklist

### JWT & AuthContext
- [ ] JWT contient les rôles dans le claim 'roles'
- [ ] mapJwtToUser() parse correctement le rôle
- [ ] AuthContext stocke le rôle dans session
- [ ] useAuth() retourne user.role

### API Communication
- [ ] Frontend envoie JWT dans Authorization header
- [ ] Backend valide le JWT correctement
- [ ] Backend extrait le rôle du JWT
- [ ] Backend refuse les requêtes sans JWT (endpoints protégés)

### Error Handling
- [ ] 401 Unauthorized = redirection login
- [ ] 403 Forbidden = redirection /unauthorized
- [ ] Erreurs affichées en logs du navigateur
- [ ] Erreurs affichées en logs du serveur

### Multi-Tenancy (Optionnel)
- [ ] Frontend envoie X-Tenant-ID dans headers
- [ ] Backend extrait le tenant du header
- [ ] TenantContext accessible dans services
- [ ] Données isolées par tenant

---

## 📊 Checklist Performance

- [ ] Pas de requêtes inutiles au backend pour vérifier permissions
- [ ] Permissions calculées localement (frontend) quand possible
- [ ] JWT parséage optimisé
- [ ] Pas d'appels récursifs dans la hiérarchie
- [ ] TenantContext cleanup évite les fuites mémoire

---

## 🚀 Avant Déploiement

- [ ] Tous les tests fonctionnels PASSENT ✅
- [ ] Pas de warnings/erreurs dans les logs
- [ ] Documentation mise à jour pour le team
- [ ] Scripts de migration DB exécutés (si nécessaire)
- [ ] Variables d'environnement configurées (JWT_SECRET, etc.)
- [ ] HTTPS configuré en production
- [ ] CORS headers corrects
- [ ] Rate limiting activé
- [ ] Audit logging activé

---

## 📞 Support & Troubleshooting

### Problème : 401 Unauthorized au login
- [ ] Vérifier credentials testator
- [ ] Vérifier que l'utilisateur existe en DB
- [ ] Vérifier que isActive = true
- [ ] Vérifier JWT_SECRET cohérent

### Problème : 403 Forbidden sur endpoint protégé
- [ ] Vérifier le rôle de l'utilisateur
- [ ] Vérifier que le JWT contient le rôle
- [ ] Vérifier la configuration SecurityConfig
- [ ] Vérifier @PreAuthorize sur la méthode

### Problème : Frontend n'affiche pas les boutons
- [ ] Vérifier usePermissions() retourne les bonnes permissions
- [ ] Vérifier que mapJwtToUser() parse le rôle
- [ ] Vérifier que AuthContext est propagé
- [ ] Vérifier les conditions dans le JSX

### Problème : TenantContext est null
- [ ] Vérifier que X-Tenant-ID est envoyé dans headers
- [ ] Vérifier le format UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
- [ ] Vérifier que TenantFilter est enregistré
- [ ] Vérifier les logs pour "Invalid tenant ID"

---

**Version**: 1.0.0  
**Complété**: 2026-04-14  
**Status**: 🎉 READY FOR TESTING
