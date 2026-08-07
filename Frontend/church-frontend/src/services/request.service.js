import { apiClient } from './http/apiClient';

function parishQuery(paroissePublicId, { page, size } = {}) {
  const params = new URLSearchParams();
  if (page != null) params.set('page', String(page));
  if (size != null) params.set('size', String(size));
  const qs = params.toString();
  return `/demandes/paroisse/${paroissePublicId}${qs ? `?${qs}` : ''}`;
}

export const requestService = {
  getAll: (options = {}) => apiClient('/demandes', {}, { auth: true, ...options }),

  getByParish: (paroissePublicId, options = {}) => {
    const { page, size, signal } = options;
    return apiClient(parishQuery(paroissePublicId, { page, size }), {}, { auth: true, signal });
  },

  getParishStats: (paroissePublicId, options = {}) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}/stats`, {}, { auth: true, signal: options.signal }),

  getById: (publicId, options = {}) =>
    apiClient(`/demandes/${publicId}`, {}, { auth: true, signal: options.signal }),

  getByTrackingCode: (code, options = {}) =>
    apiClient(`/demandes/code/${encodeURIComponent(code)}`, {}, { auth: false, signal: options.signal }),

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
