import { apiClient } from './http/apiClient';

export const requestTypeService = {
  getAll: () => apiClient('/type-demandes', {}, { auth: true }),

  getByParish: (paroissePublicId) =>
    apiClient(`/type-demandes/paroisse/${paroissePublicId}`, {}, { auth: true }),

  /** Référentiel public (sans JWT) */
  getByParishPublic: (paroissePublicId) =>
    apiClient(`/type-demandes/paroisse/${paroissePublicId}`, {}, { auth: false }),

  getById: (publicId) => apiClient(`/type-demandes/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/type-demandes', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/type-demandes/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
