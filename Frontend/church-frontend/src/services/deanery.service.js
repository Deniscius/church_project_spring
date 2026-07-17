import { apiClient } from './http/apiClient';

export const deaneryService = {
  getAll: () => apiClient('/doyennes', {}, { auth: true }),

  getById: (publicId) => apiClient(`/doyennes/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/doyennes', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/doyennes/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  remove: (publicId) => apiClient(`/doyennes/${publicId}`, { method: 'DELETE' }, { auth: true }),
};
