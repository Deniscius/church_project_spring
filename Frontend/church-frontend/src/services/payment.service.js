import { apiClient } from './http/apiClient';

export const paymentService = {
  getAll: () => apiClient('/details-paiement', {}, { auth: true }),

  getById: (publicId) => apiClient(`/details-paiement/${publicId}`, {}, { auth: true }),

  /** Paiements dérivés des demandes de la paroisse (champs fusionnés sur la demande). */
  listRowsForParish: (paroissePublicId) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}`, {}, { auth: true }),
};
