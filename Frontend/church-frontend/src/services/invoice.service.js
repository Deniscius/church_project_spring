import { mockInvoices } from '../data/mockData';

export const invoiceService = {
  getAll: async () => mockInvoices,
  getById: async (id) => mockInvoices.find((item) => item.id === Number(id)),
};
