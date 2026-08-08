import { apiClient } from './http/apiClient';

export const inscriptionService = {
  /**
   * @param {object} payload
   * @param {File} mandatCure
   * @param {File} adminCni
   */
  soumettre: (payload, mandatCure, adminCni) => {
    const form = new FormData();
    form.append(
      'payload',
      new Blob([JSON.stringify(payload)], { type: 'application/json' })
    );
    form.append('mandatCure', mandatCure);
    form.append('adminCni', adminCni);
    return apiClient('/inscriptions-paroisse', {
      method: 'POST',
      body: form,
    });
  },

  sendOtp: (email) =>
    apiClient('/inscriptions-paroisse/otp/envoyer', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  verifyOtp: (payload) =>
    apiClient('/inscriptions-paroisse/otp/verifier', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  list: () => apiClient('/inscriptions-paroisse', {}, { auth: true }),

  documentUrl: (publicId, type) => `/inscriptions-paroisse/${publicId}/documents/${type}`,

  downloadDocument: (publicId, type) =>
    apiClient(
      `/inscriptions-paroisse/${publicId}/documents/${type}`,
      {},
      { auth: true, parse: 'blob' }
    ),

  approuver: (publicId) =>
    apiClient(`/inscriptions-paroisse/${publicId}/approuver`, { method: 'POST' }, { auth: true }),

  rejeter: (publicId, motif) =>
    apiClient(
      `/inscriptions-paroisse/${publicId}/rejeter${motif ? `?motif=${encodeURIComponent(motif)}` : ''}`,
      { method: 'POST' },
      { auth: true }
    ),
};

export const comptabiliteService = {
  getCompte: (paroissePublicId) =>
    apiClient(`/comptabilite/comptes/${paroissePublicId}`, {}, { auth: true }),

  listReversements: () => apiClient('/comptabilite/reversements', {}, { auth: true }),

  listReversementsParoisse: (paroissePublicId) =>
    apiClient(`/comptabilite/reversements/paroisse/${paroissePublicId}`, {}, { auth: true }),

  demanderReversement: (payload) =>
    apiClient('/comptabilite/reversements', {
      method: 'POST',
      body: JSON.stringify(payload),
    }, { auth: true }),

  payerReversement: (publicId, payload) =>
    apiClient(`/comptabilite/reversements/${publicId}/payer`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }, { auth: true }),

  rejeterReversement: (publicId, payload) =>
    apiClient(`/comptabilite/reversements/${publicId}/rejeter`, {
      method: 'POST',
      body: JSON.stringify(payload || {}),
    }, { auth: true }),

  listAbonnements: () => apiClient('/comptabilite/abonnements', {}, { auth: true }),

  checkoutAbonnement: (paroissePublicId, plan) =>
    apiClient(
      `/comptabilite/abonnements/checkout/${paroissePublicId}${plan ? `?plan=${encodeURIComponent(plan)}` : ''}`,
      { method: 'POST' },
      { auth: true }
    ),

  activerAbonnement: (paroissePublicId, plan) =>
    apiClient(
      `/comptabilite/abonnements/${paroissePublicId}/activer${plan ? `?plan=${encodeURIComponent(plan)}` : ''}`,
      { method: 'POST' },
      { auth: true }
    ),

  prolongerAbonnement: (paroissePublicId, jours = 30) =>
    apiClient(
      `/comptabilite/abonnements/${paroissePublicId}/prolonger?jours=${encodeURIComponent(jours)}`,
      { method: 'POST' },
      { auth: true }
    ),

  annulerAbonnementPending: (abonnementPublicId) =>
    apiClient(
      `/comptabilite/abonnements/${abonnementPublicId}/annuler`,
      { method: 'POST' },
      { auth: true }
    ),

  resilierAbonnement: (paroissePublicId) =>
    apiClient(
      `/comptabilite/abonnements/${paroissePublicId}/resilier`,
      { method: 'POST' },
      { auth: true }
    ),

  /** Catalogue plateforme (support technique) cloné aux nouveaux tenants. */
  getCatalogueModele: () =>
    apiClient('/comptabilite/catalogue-modele', {}, { auth: true }),
};
