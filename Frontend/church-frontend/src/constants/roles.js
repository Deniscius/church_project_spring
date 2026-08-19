/**
 * Rôles applicatifs. Les permissions effectives ne sont PAS calculées ici :
 * elles viennent du backend dans la session authentifiée.
 */
export const ROLES = {
  CURE: 'CURE',
  SECRETAIRE: 'SECRETAIRE',
  ADMIN: 'ADMIN',
  COMPTABLE_LOCAL: 'COMPTABLE_LOCAL',
  COMPTABLE: 'COMPTABLE',
  SUPER_ADMIN: 'SUPER_ADMIN',
};

export const ROLE_LABELS = {
  CURE: 'Curé',
  SECRETAIRE: 'Secrétaire',
  ADMIN: 'Administrateur',
  COMPTABLE_LOCAL: 'Comptable paroissial',
  COMPTABLE: 'Comptable plateforme',
  SUPER_ADMIN: 'Super Administrateur',
};

export const ROLE_DESCRIPTIONS = {
  CURE: 'Curé - Consultation et validation des demandes',
  SECRETAIRE: 'Secrétaire - Demandes et encaissements en caisse',
  ADMIN: 'Administrateur - Gestion complète de sa paroisse et de son équipe',
  COMPTABLE_LOCAL: 'Comptable paroissial - Contrôle de la caisse et de la trésorerie locale',
  COMPTABLE: 'Comptable plateforme - Finances SaaS et catalogue plateforme autorisé',
  SUPER_ADMIN: 'Super Administrateur - Administration globale et intervention multi-tenant',
};

/** Noms publics des authorities RBAC du backend. */
export const PERMISSIONS = {
  DASHBOARD_VIEW: 'dashboard:view',
  DEMAND_READ: 'demand:read',
  DEMAND_EDIT: 'demand:edit',
  DEMAND_DELETE: 'demand:delete',
  DEMAND_VALIDATE: 'demand:validate',
  DEMAND_AUDIT: 'demand:audit',
  DEMAND_DATE_MANAGE: 'demand-date:manage',
  PAYMENT_READ: 'payment:read',
  PAYMENT_MANAGE: 'payment:manage',
  PAYMENT_DELETE: 'payment:delete',
  TREASURY_READ: 'treasury:read',
  TREASURY_MANAGE: 'treasury:manage',
  PAYOUT_MANAGE: 'payout:manage',
  SUBSCRIPTION_READ: 'subscription:read',
  SUBSCRIPTION_CHECKOUT: 'subscription:checkout',
  SUBSCRIPTION_ACTIVATE: 'subscription:activate',
  SUBSCRIPTION_MANAGE: 'subscription:manage',
  RECEIPT_MANAGE: 'receipt:manage',
  INVOICE_READ: 'invoice:read',
  INVOICE_MANAGE: 'invoice:manage',
  SCHEDULE_READ: 'schedule:read',
  SCHEDULE_MANAGE: 'schedule:manage',
  CELEBRATION_MANAGE: 'celebration:manage',
  CELEBRATION_SCHEDULE_MANAGE: 'celebration-schedule:manage',
  REQUEST_TYPE_READ: 'request-type:read',
  REQUEST_TYPE_MANAGE: 'request-type:manage',
  PRICING_READ: 'pricing:read',
  PRICING_MANAGE: 'pricing:manage',
  USER_MANAGE: 'user:manage',
  PARISH_READ: 'parish:read',
  PARISH_MANAGE: 'parish:manage',
  PARISH_SETTINGS_MANAGE: 'parish-settings:manage',
  PARISH_ACCESS_MANAGE: 'parish-access:manage',
  PARISH_REGISTRATION_READ: 'parish-registration:read',
  PARISH_REGISTRATION_MANAGE: 'parish-registration:manage',
  DEANERY_MANAGE: 'deanery:manage',
  PAYMENT_TYPE_MANAGE: 'payment-type:manage',
  FINANCE_READ: 'finance:read',
  FINANCE_MANAGE: 'finance:manage',
  SAAS_PLAN_READ: 'saas-plan:read',
  SAAS_PLAN_MANAGE: 'saas-plan:manage',
  PROFILE_READ: 'profile:read',
  SYSTEM_ADMIN: 'system:admin',
};

export const ROLE_TAGS = {
  CURE: 'ROLE_CURE',
  SECRETAIRE: 'ROLE_SECRETAIRE',
  ADMIN: 'ROLE_ADMIN',
  COMPTABLE_LOCAL: 'ROLE_COMPTABLE_LOCAL',
  COMPTABLE: 'ROLE_COMPTABLE',
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN',
};

/** Identité de rôle uniquement. Ne pas utiliser comme substitut aux permissions métier. */
export function isAdmin(role) {
  return role === ROLES.ADMIN || role === ROLES.SUPER_ADMIN;
}

export function isSuperAdmin(role) {
  return role === ROLES.SUPER_ADMIN;
}
