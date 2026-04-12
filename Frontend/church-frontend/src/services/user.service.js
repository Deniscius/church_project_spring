import { apiClient } from './http/apiClient';

export const userService = {
  getAll: () => apiClient('/users', {}, { auth: true }),

  getById: (publicId) => apiClient(`/users/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/users', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/users/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
