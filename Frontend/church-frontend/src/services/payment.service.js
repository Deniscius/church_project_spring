import { mockPayments } from '../data/mockData';

export const paymentService = {
  getAll: async () => mockPayments,
  getById: async (id) => mockPayments.find((item) => item.id === Number(id)),
};
