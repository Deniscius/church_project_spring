/**
 * Énumération des rôles utilisateur (utilisateurs authentifiés uniquement).
 * Hiérarchie : SUPER_ADMIN > ADMIN > SECRETAIRE > CURE
 * 
 * Note : Les fidèles n'ont pas de compte, ils accèdent via endpoints publics anonymes.
 */
export const ROLES = {
  CURE: 'CURE',                 // Curé - consultation et validation
  SECRETAIRE: 'SECRETAIRE',     // Secrétaire - gestion des demandes
  ADMIN: 'ADMIN',               // Admin local - gestion paroisse
  SUPER_ADMIN: 'SUPER_ADMIN',   // Super admin - gestion système
};

/**
 * Libellés localisés des rôles.
 */
export const ROLE_LABELS = {
  CURE: 'Curé',
  SECRETAIRE: 'Secrétaire',
  ADMIN: 'Administrateur',
  SUPER_ADMIN: 'Super Administrateur',
};

/**
 * Descriptions des rôles pour l'UI.
 */
export const ROLE_DESCRIPTIONS = {
  CURE: 'Curé - Consultation et validation des demandes',
  SECRETAIRE: 'Secrétaire - Gère les demandes et validations de sa paroisse',
  ADMIN: 'Administrateur - Gestion complète de sa paroisse et ses utilisateurs',
  SUPER_ADMIN: 'Super Administrateur - Gestion système complète, multi-paroisse',
};

/**
 * Permissions par rôle.
 * Défini le niveau d'accès granulaire pour chaque rôle.
 */
export const ROLE_PERMISSIONS = {
  SUPER_ADMIN: ['read', 'create', 'edit', 'delete', 'validate', 'admin', 'manage_users', 'manage_system'],
  ADMIN: ['read', 'create', 'edit', 'delete', 'validate', 'manage_users'],
  SECRETAIRE: ['read', 'create', 'edit', 'validate'],
  CURE: ['read', 'validate'],
};

/**
 * Hiérarchie des rôles (du plus bas au plus élevé).
 */
export const ROLE_HIERARCHY = {
  CURE: 1,
  SECRETAIRE: 2,
  ADMIN: 3,
  SUPER_ADMIN: 4,
};

/**
 * Tags pour identifier les rôles au backend.
 */
export const ROLE_TAGS = {
  CURE: 'ROLE_CURE',
  SECRETAIRE: 'ROLE_SECRETAIRE',
  ADMIN: 'ROLE_ADMIN',
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN',
};

/**
 * Vérifie si un rôle possède une permission.
 * @param {string} role - Le rôle utilisateur
 * @param {string} permission - La permission à vérifier
 * @returns {boolean}
 */
export function hasPermission(role, permission) {
  const permissions = ROLE_PERMISSIONS[role] || [];
  return permissions.includes(permission);
}

/**
 * Vérifie si un rôle can read (lecture).
 */
export function canRead(role) {
  return hasPermission(role, 'read');
}

/**
 * Vérifie si un rôle can create (créer).
 */
export function canCreate(role) {
  return hasPermission(role, 'create');
}

/**
 * Vérifie si un rôle can edit (éditer).
 */
export function canEdit(role) {
  return hasPermission(role, 'edit');
}

/**
 * Vérifie si un rôle can delete (supprimer).
 */
export function canDelete(role) {
  return hasPermission(role, 'delete');
}

/**
 * Vérifie si un rôle can validate (valider).
 */
export function canValidate(role) {
  return hasPermission(role, 'validate');
}

/**
 * Vérifie si un rôle est administrateur.
 */
export function isAdmin(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN;
}

/**
 * Vérifie si un rôle est super administrateur.
 */
export function isSuperAdmin(role) {
  return role === ROLES.SUPER_ADMIN;
}

/**
 * Vérifie la hiérarchie entre deux rôles.
 * @param {string} userRole - Le rôle de l'utilisateur
 * @param {string} requiredRole - Le rôle requis
 * @returns {boolean} true si userRole >= requiredRole
 */
export function isHierarchyGreaterOrEqual(userRole, requiredRole) {
  return (ROLE_HIERARCHY[userRole] || 0) >= (ROLE_HIERARCHY[requiredRole] || 0);
}
