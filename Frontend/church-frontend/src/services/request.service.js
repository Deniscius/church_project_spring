import { apiClient } from './http/apiClient';

export const requestService = {
  getAll: () => apiClient('/demandes', {}, { auth: true }),

  getByParish: (paroissePublicId) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}`, {}, { auth: true }),

  getById: (publicId) => apiClient(`/demandes/${publicId}`, {}, { auth: true }),

  getByTrackingCode: (code) =>
    apiClient(`/demandes/code/${encodeURIComponent(code)}`, {}, { auth: false }),

  create: (payload) =>
    apiClient('/demandes', { method: 'POST', body: JSON.stringify(payload) }, { auth: false }),

  update: (publicId, payload) =>
    apiClient(`/demandes/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  remove: (publicId) => apiClient(`/demandes/${publicId}`, { method: 'DELETE' }, { auth: true }),
};
