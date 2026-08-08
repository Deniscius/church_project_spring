import { apiClient } from './http/apiClient';

export const invoiceService = {
  getAll: () => apiClient('/facture', {}, { auth: true }),

  getById: (publicId) => apiClient(`/facture/${publicId}`, {}, { auth: true }),

  getByTrackingCode: (code) =>
    apiClient(`/facture/code-suivie/${encodeURIComponent(code)}`, {}, { auth: false }),

  /** Factures de la paroisse (endpoint dédié, sans double fetch global). */
  listForParish: (paroissePublicId, options = {}) =>
    apiClient(`/facture/paroisse/${paroissePublicId}`, {}, { auth: true, signal: options.signal }),
};
