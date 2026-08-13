/**
 * Énumération des rôles utilisateur (utilisateurs authentifiés uniquement).
 * Hiérarchie : SUPER_ADMIN > ADMIN > SECRETAIRE > CURE
 * 
 * Note : Les fidèles n'ont pas de compte, ils accèdent via endpoints publics anonymes.
 */
export const ROLES = {
  CURE: 'CURE',
  SECRETAIRE: 'SECRETAIRE',
  ADMIN: 'ADMIN',
  COMPTABLE_LOCAL: 'COMPTABLE_LOCAL',
  COMPTABLE: 'COMPTABLE',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

/**
 * Libellés localisés des rôles.
 */
export const ROLE_LABELS = {
  CURE: 'Curé',
  SECRETAIRE: 'Secrétaire',
  ADMIN: 'Administrateur',
  COMPTABLE_LOCAL: 'Comptable paroissial',
  COMPTABLE: 'Comptable plateforme',
  SUPER_ADMIN: 'Super Administrateur',
};

/**
 * Descriptions des rôles pour l'UI.
 */
export const ROLE_DESCRIPTIONS = {
  CURE: 'Curé - Consultation et validation des demandes',
  SECRETAIRE: 'Secrétaire - Demandes et encaissements en caisse',
  ADMIN: 'Administrateur - Gestion complète de sa paroisse et ses utilisateurs',
  COMPTABLE_LOCAL: 'Comptable paroissial - Contrôle de la caisse et de la trésorerie locale',
  COMPTABLE: 'Comptable SaaS - Finances, abonnements et catalogue plateforme',
  SUPER_ADMIN:
    'Super Administrateur — plateforme (paroisses, accès, finances) ; intervention ponctuelle sur un tenant en cas de souci',
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
  /**
   * Trésorerie de la paroisse : solde et demandes de reversement.
   * Distinct de PAYMENT_READ, qui ne couvre que le paiement des intentions.
   * Aligné sur le backend, qui réserve /comptabilite/comptes à l'ADMIN.
   */
  TREASURY_READ: 'treasury:read',
  /** Personnalisation du reçu PDF (logo / en-tête) — admin local après activation. */
  RECEIPT_MANAGE: 'receipt:manage',
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
  DEANERY_MANAGE: 'deanery:manage',
  PAYMENT_TYPE_MANAGE: 'payment-type:manage',
  /** Consulter inscriptions, abonnements et reversements. */
  FINANCE_READ: 'finance:read',
  /** Exécuter les mouvements d'argent : réservé au comptable plateforme. */
  FINANCE_MANAGE: 'finance:manage',
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
  // Séparation des pouvoirs : le super admin compose l'équipe et supervise les
  // finances, mais leur exécution appartient au comptable.
  SUPER_ADMIN: [
    PERMISSIONS.USER_MANAGE,
    PERMISSIONS.PARISH_MANAGE,
    PERMISSIONS.PARISH_ACCESS_MANAGE,
    PERMISSIONS.DEANERY_MANAGE,
    PERMISSIONS.PAYMENT_TYPE_MANAGE,
    PERMISSIONS.FINANCE_READ,
    // Activation manuelle depuis l’annuaire (prospect → abonnée).
    PERMISSIONS.FINANCE_MANAGE,
    // Intervention multi-tenant : outils paroissiaux pour contrôle / support.
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.DEMAND_EDIT,
    PERMISSIONS.DEMAND_DELETE,
    PERMISSIONS.DEMAND_VALIDATE,
    PERMISSIONS.PAYMENT_MANAGE,
    PERMISSIONS.TREASURY_READ,
    PERMISSIONS.RECEIPT_MANAGE,
    // Catalogue plateforme (même écran que le comptable).
    PERMISSIONS.SCHEDULE_READ,
    PERMISSIONS.SCHEDULE_MANAGE,
    PERMISSIONS.REQUEST_TYPE_READ,
    PERMISSIONS.REQUEST_TYPE_MANAGE,
    PERMISSIONS.PRICING_READ,
    PERMISSIONS.PRICING_MANAGE,
    PERMISSIONS.PROFILE_READ,
  ],
  COMPTABLE: [
    PERMISSIONS.FINANCE_READ,
    PERMISSIONS.FINANCE_MANAGE,
    // Catalogue plateforme : grille clonée vers les nouvelles paroisses.
    PERMISSIONS.SCHEDULE_READ,
    PERMISSIONS.SCHEDULE_MANAGE,
    PERMISSIONS.REQUEST_TYPE_READ,
    PERMISSIONS.REQUEST_TYPE_MANAGE,
    PERMISSIONS.PRICING_READ,
    PERMISSIONS.PRICING_MANAGE,
    PERMISSIONS.PROFILE_READ,
  ],
  ADMIN: [
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.DEMAND_EDIT,
    PERMISSIONS.DEMAND_DELETE,
    PERMISSIONS.DEMAND_VALIDATE,
    PERMISSIONS.PAYMENT_MANAGE,
    PERMISSIONS.TREASURY_READ,
    PERMISSIONS.RECEIPT_MANAGE,
    PERMISSIONS.SCHEDULE_MANAGE,
    PERMISSIONS.REQUEST_TYPE_MANAGE,
    PERMISSIONS.PRICING_MANAGE,
    PERMISSIONS.USER_MANAGE,
  ],
  COMPTABLE_LOCAL: [
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.TREASURY_READ,
  ],
  SECRETAIRE: [
    ...PARISH_READ_PERMISSIONS,
    PERMISSIONS.DEMAND_EDIT,
    PERMISSIONS.PAYMENT_MANAGE,
    PERMISSIONS.TREASURY_READ,
  ],
  CURE: [...PARISH_READ_PERMISSIONS, PERMISSIONS.DEMAND_VALIDATE],
};

/**
 * Hiérarchie des rôles (du plus bas au plus élevé).
 */
export const ROLE_HIERARCHY = {
  CURE: 1,
  SECRETAIRE: 2,
  COMPTABLE_LOCAL: 3,
  ADMIN: 4,
  COMPTABLE: 5,
  SUPER_ADMIN: 6,
};

/**
 * Tags pour identifier les rôles au backend.
 */
export const ROLE_TAGS = {
  CURE: 'ROLE_CURE',
  SECRETAIRE: 'ROLE_SECRETAIRE',
  ADMIN: 'ROLE_ADMIN',
  COMPTABLE_LOCAL: 'ROLE_COMPTABLE_LOCAL',
  COMPTABLE: 'ROLE_COMPTABLE',
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
