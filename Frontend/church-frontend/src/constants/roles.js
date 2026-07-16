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
export const PERMISSIONS = {
  DASHBOARD_VIEW: 'dashboard:view',
  DEMAND_READ: 'demand:read',
  DEMAND_EDIT: 'demand:edit',
  DEMAND_DELETE: 'demand:delete',
  DEMAND_VALIDATE: 'demand:validate',
  PAYMENT_READ: 'payment:read',
  PAYMENT_MANAGE: 'payment:manage',
  INVOICE_READ: 'invoice:read',
  SCHEDULE_READ: 'schedule:read',
  SCHEDULE_MANAGE: 'schedule:manage',
  REQUEST_TYPE_READ: 'request-type:read',
  REQUEST_TYPE_MANAGE: 'request-type:manage',
  PRICING_READ: 'pricing:read',
  PRICING_MANAGE: 'pricing:manage',
  USER_MANAGE: 'user:manage',
  PARISH_MANAGE: 'parish:manage',
  PARISH_ACCESS_MANAGE: 'parish-access:manage',
  LOCALITY_MANAGE: 'locality:manage',
  PAYMENT_TYPE_MANAGE: 'payment-type:manage',
  PROFILE_READ: 'profile:read',
};

const PARISH_READ_PERMISSIONS = [
  PERMISSIONS.DASHBOARD_VIEW,
  PERMISSIONS.DEMAND_READ,
  PERMISSIONS.PAYMENT_READ,
  PERMISSIONS.INVOICE_READ,
  PERMISSIONS.SCHEDULE_READ,
  PERMISSIONS.REQUEST_TYPE_READ,
  PERMISSIONS.PRICING_READ,
  PERMISSIONS.PROFILE_READ,
];

export const ROLE_PERMISSIONS = {
  SUPER_ADMIN: [
    PERMISSIONS.USER_MANAGE,
    PERMISSIONS.PARISH_MANAGE,
    PERMISSIONS.PARISH_ACCESS_MANAGE,
    PERMISSIONS.LOCALITY_MANAGE,
    PERMISSIONS.PAYMENT_TYPE_MANAGE,
  ],
  ADMIN: [
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.DEMAND_EDIT,
    PERMISSIONS.DEMAND_DELETE,
    PERMISSIONS.DEMAND_VALIDATE,
    PERMISSIONS.PAYMENT_MANAGE,
    PERMISSIONS.SCHEDULE_MANAGE,
    PERMISSIONS.REQUEST_TYPE_MANAGE,
    PERMISSIONS.PRICING_MANAGE,
    PERMISSIONS.USER_MANAGE,
  ],
  SECRETAIRE: [
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.DEMAND_EDIT,
    PERMISSIONS.PAYMENT_MANAGE,
  ],
  CURE: [...PARISH_READ_PERMISSIONS, PERMISSIONS.DEMAND_VALIDATE],
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
  return role in ROLE_PERMISSIONS;
}

/**
 * Vérifie si un rôle can create (créer).
 */
export function canCreate(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN || role === ROLES.SECRETAIRE;
}

/**
 * Vérifie si un rôle can edit (éditer).
 */
export function canEdit(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN || role === ROLES.SECRETAIRE;
}

/**
 * Vérifie si un rôle can delete (supprimer).
 */
export function canDelete(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN;
}

/**
 * Vérifie si un rôle can validate (valider).
 */
export function canValidate(role) {
  return hasPermission(role, PERMISSIONS.DEMAND_VALIDATE);
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
