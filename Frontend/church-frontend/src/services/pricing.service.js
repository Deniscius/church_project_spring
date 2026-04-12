import { apiClient } from './http/apiClient';

export const pricingService = {
  getAll: () => apiClient('/forfait-tarifs', {}, { auth: true }),

  getById: (publicId) => apiClient(`/forfait-tarifs/${publicId}`, {}, { auth: true }),

  getByTypeDemande: (typeDemandePublicId) =>
    apiClient(`/forfait-tarifs/type-demande/${typeDemandePublicId}`, {}, { auth: true }),

  getActiveByTypeDemande: (typeDemandePublicId) =>
    apiClient(`/forfait-tarifs/type-demande/${typeDemandePublicId}/actifs`, {}, { auth: true }),

  getActiveByTypeDemandePublic: (typeDemandePublicId) =>
    apiClient(`/forfait-tarifs/type-demande/${typeDemandePublicId}/actifs`, {}, { auth: false }),

  create: (payload) =>
    apiClient('/forfait-tarifs', { method: 'POST', body: JSON.stringify(payload) }, { auth: true }),

  update: (publicId, payload) =>
    apiClient(`/forfait-tarifs/${publicId}`, { method: 'PUT', body: JSON.stringify(payload) }, { auth: true }),
};
