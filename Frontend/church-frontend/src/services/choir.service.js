import { apiClient } from './http/apiClient';

export const choirService = {
  list: (parishId) => apiClient(`/paroisses/${parishId}/chorales`, {}, { auth: true }),
  create: (parishId, payload) => apiClient(`/paroisses/${parishId}/chorales`, {
    method: 'POST', body: JSON.stringify(payload),
  }, { auth: true }),
  update: (choirId, payload) => apiClient(`/chorales/${choirId}`, {
    method: 'PUT', body: JSON.stringify(payload),
  }, { auth: true }),
  remove: (choirId) => apiClient(`/chorales/${choirId}`, { method: 'DELETE' }, { auth: true }),
  invite: (choirId) => apiClient(`/chorales/${choirId}/invitations`, { method: 'POST' }, { auth: true }),
};
