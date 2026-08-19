import { apiClient } from './http/apiClient';

export const planSaasService = {
  listPublic: () => apiClient('/plans-saas/public'),

  listAll: () => apiClient('/plans-saas', {}, { auth: true }),

  create: (payload) =>
    apiClient(
      '/plans-saas',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
      { auth: true }
    ),

  update: (publicId, payload) =>
    apiClient(
      `/plans-saas/${publicId}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload),
      },
      { auth: true }
    ),
};
