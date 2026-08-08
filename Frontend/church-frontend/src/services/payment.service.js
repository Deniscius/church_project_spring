import { apiClient } from './http/apiClient';

export const paymentService = {
  getAll: () => apiClient('/details-paiement', {}, { auth: true }),

  getById: (publicId) => apiClient(`/details-paiement/${publicId}`, {}, { auth: true }),

  /** Paiements dérivés des demandes de la paroisse (champs fusionnés sur la demande). */
  listRowsForParish: (paroissePublicId) =>
    apiClient(`/demandes/paroisse/${paroissePublicId}`, {}, { auth: true }),

  /** Devis des frais FedaPay pour un code de suivi (public). */
  quoteByTrackingCode: (codeSuivie) =>
    apiClient(`/paiements/quote/${encodeURIComponent(codeSuivie)}`),

  /** Crée / reprend une session FedaPay et renvoie paymentUrl (public). */
  checkoutByTrackingCode: (codeSuivie) =>
    apiClient(`/paiements/checkout/${encodeURIComponent(codeSuivie)}`, {
      method: 'POST',
    }),

  /** Encaissement espèces au secrétariat — hors solde de reversement. */
  encaisserCaisse: (demandePublicId) =>
    apiClient(`/details-paiement/caisse/${demandePublicId}`, { method: 'POST' }, { auth: true }),

  /** Journal de caisse locale de la paroisse. */
  resumeCaisse: (paroissePublicId) =>
    apiClient(`/details-paiement/caisse/paroisse/${paroissePublicId}`, {}, { auth: true }),
};
