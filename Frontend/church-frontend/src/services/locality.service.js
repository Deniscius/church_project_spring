import { apiClient } from './http/apiClient';

export const localityService = {
  getAll: () => apiClient('/localites', {}, { auth: true }),

  getById: (publicId) => apiClient(`/localites/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/localites', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/localites/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  remove: (publicId) => apiClient(`/localites/${publicId}`, { method: 'DELETE' }, { auth: true }),
};
