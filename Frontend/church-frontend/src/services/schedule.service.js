import { apiClient } from './http/apiClient';

export const scheduleService = {
  /** Accueil : horaires des paroisses à abonnement actif. */
  listPublicForActiveParishes: () =>
    apiClient('/horaires/public/paroisses-actives', {}, { auth: false }),
  getByParish: (paroissePublicId) =>
    apiClient(`/horaires/paroisse/${paroissePublicId}`, {}, { auth: true }),

  getByParishPublic: (paroissePublicId) =>
    apiClient(`/horaires/paroisse/${paroissePublicId}`, {}, { auth: false }),

  /** Programme résolu (dates précises + messe unique). */
  getProgramme: (paroissePublicId, { debut, fin, signal } = {}) => {
    const params = new URLSearchParams();
    if (debut) params.set('debut', debut);
    if (fin) params.set('fin', fin);
    const qs = params.toString();
    return apiClient(
      `/horaires/paroisse/${paroissePublicId}/programme${qs ? `?${qs}` : ''}`,
      { signal },
      { auth: true }
    );
  },

  getAll: () => apiClient('/horaires', {}, { auth: true }),

  getById: (publicId) => apiClient(`/horaires/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/horaires', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/horaires/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  remove: (publicId) => apiClient(`/horaires/${publicId}`, { method: 'DELETE' }, { auth: true }),
};
