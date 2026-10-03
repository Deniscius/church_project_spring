import { apiClient } from './http/apiClient';

export const pastoralYearService = {
  getByParish: (paroissePublicId) =>
    apiClient(`/annees-pastorales/paroisse/${paroissePublicId}`, {}, { auth: true }),

  getPublishedByParish: (paroissePublicId) =>
    apiClient(`/annees-pastorales/paroisse/${paroissePublicId}/publiee`, {}, { auth: true }),

  getById: (publicId) =>
    apiClient(`/annees-pastorales/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient(
      '/annees-pastorales',
      { method: 'POST', body: JSON.stringify(payload) },
      { auth: true }
    ),

  update: (publicId, payload) =>
    apiClient(
      `/annees-pastorales/${publicId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      { auth: true }
    ),

  publish: (publicId, version) =>
    apiClient(
      `/annees-pastorales/${publicId}/publication`,
      { method: 'POST', body: JSON.stringify({ version }) },
      { auth: true }
    ),

  close: (publicId, version) =>
    apiClient(
      `/annees-pastorales/${publicId}/cloture`,
      { method: 'POST', body: JSON.stringify({ version }) },
      { auth: true }
    ),

  remove: (publicId, version) =>
    apiClient(
      `/annees-pastorales/${publicId}?version=${encodeURIComponent(version)}`,
      { method: 'DELETE' },
      { auth: true }
    ),
};
