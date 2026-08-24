import { apiClient } from './http/apiClient';

function parishQuery(paroissePublicId, { page = 0, size = 20, includeDeleted = false } = {}) {
  const params = new URLSearchParams();
  params.set('page', String(page));
  params.set('size', String(size));
  if (includeDeleted) params.set('includeDeleted', 'true');
  return `/demandes/paroisse/${paroissePublicId}?${params.toString()}`;
}

export const requestService = {
  getAll: (options = {}) => apiClient('/demandes', {}, { auth: true, ...options }),

  /**
   * Audit plateforme (COMPTABLE / SUPER_ADMIN) : toutes les paroisses, paginé.
   * includeDeleted=true par défaut côté API.
   */
  getAllPlatform: (options = {}) => {
    const {
      page = 0,
      size = 20,
      includeDeleted = true,
      signal,
    } = options;
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('size', String(size));
    params.set('includeDeleted', includeDeleted ? 'true' : 'false');
    return apiClient(`/demandes?${params.toString()}`, {}, { auth: true, signal });
  },

  /** Toujours paginé côté API (PageResponse). */
  getByParish: (paroissePublicId, options = {}) => {
    const { page = 0, size = 20, includeDeleted = false, signal } = options;
    return apiClient(
      parishQuery(paroissePublicId, { page, size, includeDeleted }),
      {},
      { auth: true, signal }
    );
  },

  /** Recherche serveur : code, demandeur ou téléphone, sur toute la paroisse. */
  searchByParish: (paroissePublicId, options = {}) => {
    const {
      query = '',
      page = 0,
      size = 20,
      includeDeleted = false,
      signal,
    } = options;
    const params = new URLSearchParams();
    params.set('q', query);
    params.set('page', String(page));
    params.set('size', String(size));
    params.set('includeDeleted', includeDeleted ? 'true' : 'false');
    return apiClient(
      `/demandes/paroisse/${paroissePublicId}/recherche?${params.toString()}`,
      {},
      { auth: true, signal }
    );
  },

  getParishStats: (paroissePublicId, options = {}) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}/stats`, {}, { auth: true, signal: options.signal }),

  getById: (publicId, options = {}) =>
    apiClient(`/demandes/${publicId}`, {}, { auth: true, signal: options.signal }),

  getByTrackingCode: (code, options = {}) =>
    apiClient(`/demandes/code/${encodeURIComponent(code)}`, {}, { auth: false, signal: options.signal }),

  /**
   * Public : reçu PDF.
   * Requête GET CORS simple, sans cookie ni header CSRF inutile.
   */
  fetchReceiptPdf: async (code, options = {}) => {
    const headers = { Accept: 'application/pdf' };
    const clientOptions = {
      auth: false,
      parse: 'blob',
      credentials: 'omit',
      signal: options.signal,
    };

    try {
      return await apiClient(
        `/demandes/code/${encodeURIComponent(code)}/recu`,
        { headers },
        clientOptions
      );
    } catch (err) {
      if (err?.status === 404) {
        return apiClient(
          `/demandes/code/${encodeURIComponent(code)}/recu.pdf`,
          { headers },
          clientOptions
        );
      }
      throw err;
    }
  },

  /** Public : téléphone (E.164) → codes de suivi associés, sans données personnelles. */
  lookupByPhone: (telephone) =>
    apiClient(
      '/demandes/suivi/par-telephone',
      { method: 'POST', body: JSON.stringify({ telephone }) },
      { auth: false }
    ),

  /** Public : change le mode de paiement tant que la demande n'est pas payée. */
  updateTypePaiementByTrackingCode: (code, typePaiementPublicId) =>
    apiClient(
      `/demandes/code/${encodeURIComponent(code)}/type-paiement`,
      {
        method: 'PATCH',
        body: JSON.stringify({ typePaiementPublicId }),
      },
      { auth: false }
    ),

  create: (payload) =>
    apiClient('/demandes', { method: 'POST', body: JSON.stringify(payload) }, { auth: false }),

  update: (publicId, payload) =>
    apiClient(`/demandes/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  updateValidation: (publicId, statut) =>
    apiClient(
      `/demandes/${publicId}/validation`,
      { method: 'PATCH', body: JSON.stringify({ statut }) },
      { auth: true }
    ),

  updateIntention: (publicId, intention) =>
    apiClient(
      `/demandes/${publicId}/intention`,
      { method: 'PATCH', body: JSON.stringify({ intention }) },
      { auth: true }
    ),

  remove: (publicId) => apiClient(`/demandes/${publicId}`, { method: 'DELETE' }, { auth: true }),

  getDeletedByParish: (paroissePublicId, options = {}) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}/supprimees`, {}, { auth: true, signal: options.signal }),
};
