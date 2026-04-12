import { apiClient } from './http/apiClient';

export const invoiceService = {
  getAll: () => apiClient('/facture', {}, { auth: true }),

  getById: (publicId) => apiClient(`/facture/${publicId}`, {}, { auth: true }),

  getByTrackingCode: (code) =>
    apiClient(`/facture/code-suivie/${encodeURIComponent(code)}`, {}, { auth: false }),

  /** Factures dont la demande appartient à la paroisse (filtrage côté client). */
  async listForParish(paroissePublicId) {
    const [demandes, factures] = await Promise.all([
      apiClient(`/demandes/paroisse/${paroissePublicId}`, {}, { auth: true }),
      apiClient('/facture', {}, { auth: true }),
    ]);
    const demandeIds = new Set(demandes.map((d) => d.publicId));
    return factures.filter((f) => demandeIds.has(f.demandePublicId));
  },
};
