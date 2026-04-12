import { apiClient } from './http/apiClient';

export const parishService = {
  getAll: () => apiClient('/paroisses', {}, { auth: true }),

  /** Liste publique (référentiel formulaire) — sans JWT si le backend l’autorise */
  getAllPublic: () => apiClient('/paroisses', {}, { auth: false }),

  getById: (publicId) => apiClient(`/paroisses/${publicId}`, {}, { auth: true }),

  create: (payload) =>
    apiClient('/paroisses', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/paroisses/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
