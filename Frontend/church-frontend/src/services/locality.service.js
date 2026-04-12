import { apiClient } from './http/apiClient';

export const localityService = {
  getAll: () => apiClient('/localites', {}, { auth: true }),

  getById: (publicId) => apiClient(`/localites/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/localites', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),
};
