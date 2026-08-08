import { apiClient } from './http/apiClient';

export const paymentTypeService = {
  getAll: () => apiClient('/type-paiement', {}, { auth: true }),

  getAllPublic: () => apiClient('/type-paiement', {}, { auth: false }),

  getById: (publicId) => apiClient(`/type-paiement/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/type-paiement', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/type-paiement/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),

  remove: (publicId) => apiClient(`/type-paiement/${publicId}`, { method: 'DELETE' }, { auth: true }),
};
