import { apiClient } from './http/apiClient';

export const scheduleService = {
  getByParish: (paroissePublicId) =>
    apiClient(`/horaires/paroisse/${paroissePublicId}`, {}, { auth: true }),

  getByParishPublic: (paroissePublicId) =>
    apiClient(`/horaires/paroisse/${paroissePublicId}`, {}, { auth: false }),

  getAll: () => apiClient('/horaires', {}, { auth: true }),

  getById: (publicId) => apiClient(`/horaires/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/horaires', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/horaires/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
