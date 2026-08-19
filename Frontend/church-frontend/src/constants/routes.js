/**
 * Chemins de l’application — source unique pour menus, liens et routeurs.
 * Préférer `ROUTES.*` plutôt que des chaînes en dur.
 */
export const ROUTES = {
  // Public
  HOME: '/',
  NEW_REQUEST: '/demande',
  REQUEST_RECAP: '/demande/recapitulatif',
  REQUEST_CONFIRMATION: '/demande/confirmation',
  TRACKING: '/suivi',
  TRACKING_RESULT: '/suivi/resultat',
  PUBLIC_SCHEDULES: '/horaires',
  OFFERS: '/offres',
  PARISH_REGISTRATION: '/inscription-paroisse',
  SITE_MAP: '/plan-du-site',
  PUBLIC_INVOICE: '/facture',
  PUBLIC_PAYMENT: '/paiement',
  PUBLIC_PAYMENT_RETURN: '/paiement/retour',

  // Auth
  LOGIN: '/admin/login',
  UNAUTHORIZED: '/unauthorized',

  // Paroisse (tenant)
  DASHBOARD: '/admin/dashboard',
  DAILY_PROGRAMME: '/admin/programmations/:date',
  REQUESTS: '/admin/demandes',
  REQUEST_DETAILS: '/admin/demandes/:id',
  REQUEST_EDIT: '/admin/demandes/:id/modifier',
  CELEBRATION_SHEET: '/admin/feuille-intentions',
  PAYMENTS: '/admin/paiements',
  PAYMENT_DETAILS: '/admin/paiements/:id',
  INVOICES: '/admin/factures',
  INVOICE_DETAILS: '/admin/factures/:id',
  TREASURY: '/admin/tresorerie',
  RECEIPT: '/admin/recu',
  SCHEDULES: '/admin/horaires',
  SCHEDULE_CREATE: '/admin/horaires/nouveau',
  SCHEDULE_EDIT: '/admin/horaires/:id/modifier',
  REQUEST_TYPES: '/admin/types-demandes',
  REQUEST_TYPE_CREATE: '/admin/types-demandes/nouveau',
  REQUEST_TYPE_EDIT: '/admin/types-demandes/:id/modifier',
  PRICING: '/admin/forfaits',
  PRICING_CREATE: '/admin/forfaits/nouveau',
  PRICING_EDIT: '/admin/forfaits/:id/modifier',
  TEAM: '/admin/equipe',
  TEAM_CREATE: '/admin/equipe/nouveau',
  TEAM_EDIT: '/admin/equipe/:id/modifier',
  PROFILE: '/admin/profil',

  // Plateforme (super admin / comptable)
  PARISH_INSCRIPTIONS: '/admin/inscriptions-paroisse',
  SUBSCRIPTIONS: '/admin/abonnements',
  SAAS_PRICING: '/admin/tarification-saas',
  REVERSEMENTS: '/admin/reversements',
  CATALOGUE_MODELE: '/admin/catalogue-modele',
  /** Audit global des demandes (toutes paroisses, y compris archivées). */
  PLATFORM_DEMANDES: '/admin/demandes-plateforme',
  PARISHES: '/admin/paroisses',
  PARISH_CREATE: '/admin/paroisses/nouvelle',
  PARISH_EDIT: '/admin/paroisses/:id/modifier',
  USERS: '/admin/utilisateurs',
  USER_CREATE: '/admin/utilisateurs/nouveau',
  USER_EDIT: '/admin/utilisateurs/:id/modifier',
  PARISH_ACCESS: '/admin/acces-paroisses',
  DEANERIES: '/admin/doyennes',
  PAYMENT_TYPES: '/admin/types-paiement',
};

/** Remplace `:id` / `:codeSuivie` dans un chemin ROUTES. */
export function routePath(template, params = {}) {
  return String(template).replace(/:([A-Za-z]+)/g, (_, key) => {
    const value = params[key];
    return value == null ? `:${key}` : encodeURIComponent(String(value));
  });
}
