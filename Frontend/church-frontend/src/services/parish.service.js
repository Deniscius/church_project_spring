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
 * Liste publique des paroisses actives (sans RIB / contacts sensibles).
 * GET /paroisses/public
 */
getAllPublic: () =>
  apiClient(`${PUBLIC_API_BASE}/public`, {}, { auth: false }),

/**
 * @deprecated Ne plus appeler : GET /paroisses/{id} exige une auth et peut exposer le RIB.
 * Utiliser getAllPublic() puis filtrer, ou getById() côté admin authentifié.
 */
getByIdPublic: (publicId) =>
  apiClient(`${PUBLIC_API_BASE}/public`, {}, { auth: false }).then((list) => {
    const found = Array.isArray(list)
      ? list.find((p) => p.publicId === publicId)
      : null;
    if (!found) {
      const err = new Error('Paroisse introuvable');
      err.status = 404;
      throw err;
    }
    return found;
  }),

  // ========== ENDPOINTS ADMIN (avec auth) ==========

  /**
   * Crée une nouvelle paroisse (SUPER_ADMIN)
   * POST /paroisses
   */
  create: (payload) =>
    apiClient(PUBLIC_API_BASE,
      { method: 'POST', body: JSON.stringify(payload) }, 
      { auth: true }
    ),

  /**
   * Récupère toutes les paroisses (avec auth)
   * GET /paroisses
   */
  getAll: () => 
    apiClient(PUBLIC_API_BASE, {}, { auth: true }),

  /**
   * Récupère une paroisse spécifique
   * GET /paroisses/{id}
   */
  getById: (paroisseId) => 
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}`, {}, { auth: true }),

  /**
   * Met à jour une paroisse (SUPER_ADMIN)
   * PUT /paroisses/{id}
   */
  update: (paroisseId, payload) =>
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}`,
      { method: 'PUT', body: JSON.stringify(payload) }, 
      { auth: true }
    ),

  /**
   * Met à jour les coordonnées et le RIB de la paroisse (ADMIN local)
   * PATCH /paroisses/{id}/coordonnees
   */
  updateCoordonnees: (paroisseId, payload) =>
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}/coordonnees`,
      { method: 'PATCH', body: JSON.stringify(payload) },
      { auth: true }
    ),

  uploadLogo: (paroisseId, file) => {
    const form = new FormData();
    form.append('logo', file);
    return apiClient(`${PUBLIC_API_BASE}/${paroisseId}/logo`,
      { method: 'POST', body: form },
      { auth: true }
    );
  },

  removeLogo: (paroisseId) =>
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}/logo`,
      { method: 'DELETE' },
      { auth: true }
    ),

  fetchLogoBlob: (paroisseId) =>
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}/logo`, {}, { auth: true, parse: 'blob' }),

  /** Aperçu PDF modèle du reçu (avec logo) — admin. */
  fetchReceiptSamplePdf: async (paroisseId) => {
    try {
      return await apiClient(`${PUBLIC_API_BASE}/${paroisseId}/recu-modele`, {}, { auth: true, parse: 'blob' });
    } catch (err) {
      if (err?.status === 404) {
        return apiClient(`${PUBLIC_API_BASE}/${paroisseId}/recu-modele.pdf`, {}, { auth: true, parse: 'blob' });
      }
      throw err;
    }
  },

  /**
   * Désactive une paroisse (SUPER_ADMIN)
   * DELETE /paroisses/{id}
   */
  delete: (paroisseId) =>
    apiClient(`${PUBLIC_API_BASE}/${paroisseId}`,
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
