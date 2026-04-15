import { apiClient } from './http/apiClient';

const ADMIN_API_BASE = '/admin/paroisses';
const PUBLIC_API_BASE = '/paroisses';

/**
 * Service pour la gestion des paroisses
 * Support endpoints publics + admin
 */
export const parishService = {
  // ========== ENDPOINTS PUBLICS (sans auth) ==========

  /**
   * Liste publique des paroisses actives
   * Utilisé pour formulaire de demande publique
   */
  getAllPublic: () => 
    apiClient(PUBLIC_API_BASE, {}, { auth: false }),

  getByIdPublic: (publicId) => 
    apiClient(`${PUBLIC_API_BASE}/${publicId}`, {}, { auth: false }),

  // ========== ENDPOINTS ADMIN (avec auth) ==========

  /**
   * Crée une nouvelle paroisse (SUPER_ADMIN)
   * POST /admin/paroisses
   */
  create: (payload) =>
    apiClient(ADMIN_API_BASE, 
      { method: 'POST', body: JSON.stringify(payload) }, 
      { auth: true }
    ),

  /**
   * Récupère toutes les paroisses (avec auth)
   * GET /admin/paroisses
   */
  getAll: () => 
    apiClient(ADMIN_API_BASE, {}, { auth: true }),

  /**
   * Récupère une paroisse spécifique
   * GET /admin/paroisses/{id}
   */
  getById: (paroisseId) => 
    apiClient(`${ADMIN_API_BASE}/${paroisseId}`, {}, { auth: true }),

  /**
   * Met à jour une paroisse (SUPER_ADMIN)
   * PUT /admin/paroisses/{id}
   */
  update: (paroisseId, payload) =>
    apiClient(`${ADMIN_API_BASE}/${paroisseId}`, 
      { method: 'PUT', body: JSON.stringify(payload) }, 
      { auth: true }
    ),

  /**
   * Désactive une paroisse (SUPER_ADMIN)
   * DELETE /admin/paroisses/{id}
   */
  delete: (paroisseId) =>
    apiClient(`${ADMIN_API_BASE}/${paroisseId}`, 
      { method: 'DELETE' }, 
      { auth: true }
    ),

  /**
   * Assigne un admin local à une paroisse (SUPER_ADMIN)
   * POST /admin/paroisses/{id}/assign-admin
   */
  assignAdmin: (paroisseId, adminData) =>
    apiClient(`${ADMIN_API_BASE}/${paroisseId}/assign-admin`, 
      { method: 'POST', body: JSON.stringify(adminData) }, 
      { auth: true }
    ),

  /**
   * Récupère les admins d'une paroisse (SUPER_ADMIN)
   * GET /admin/paroisses/{id}/admins
   */
  getAdmins: (paroisseId) =>
    apiClient(`${ADMIN_API_BASE}/${paroisseId}/admins`, {}, { auth: true }),

  /**
   * Récupère les statistiques des paroisses (SUPER_ADMIN)
   * GET /admin/paroisses/stats/count
   */
  getStats: () =>
    apiClient(`${ADMIN_API_BASE}/stats/count`, {}, { auth: true }),
};
