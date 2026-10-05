import { apiClient } from './http/apiClient';

export const pastoralActorService = {
  getByParish: (paroissePublicId, filters = {}) => {
    const params = new URLSearchParams();
    if (typeof filters.actif === 'boolean') {
      params.set('actif', String(filters.actif));
    }
    if (filters.q?.trim()) {
      params.set('q', filters.q.trim());
    }
    const query = params.toString();
    return apiClient(
      `/acteurs-pastoraux/paroisse/${paroissePublicId}${query ? `?${query}` : ''}`,
      {},
      { auth: true }
    );
  },

  getById: (publicId) =>
    apiClient(`/acteurs-pastoraux/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient(
      '/acteurs-pastoraux',
      { method: 'POST', body: JSON.stringify(payload) },
      { auth: true }
    ),

  update: (publicId, payload) =>
    apiClient(
      `/acteurs-pastoraux/${publicId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      { auth: true }
    ),

  setStatus: (publicId, actif, version) =>
    apiClient(
      `/acteurs-pastoraux/${publicId}/statut`,
      { method: 'POST', body: JSON.stringify({ actif, version }) },
      { auth: true }
    ),

  remove: (publicId, version) =>
    apiClient(
      `/acteurs-pastoraux/${publicId}?version=${encodeURIComponent(version)}`,
      { method: 'DELETE' },
      { auth: true }
    ),
};
