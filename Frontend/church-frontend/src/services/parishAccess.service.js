import { apiClient } from './http/apiClient';

export const parishAccessService = {
  getAll: () => apiClient('/paroisse-access', {}, { auth: true }),

  getByUser: (userPublicId) =>
    apiClient(`/paroisse-access/user/${userPublicId}`, {}, { auth: true }),

  getByParish: (paroissePublicId) =>
    apiClient(`/paroisse-access/paroisse/${paroissePublicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/paroisse-access', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/paroisse-access/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
