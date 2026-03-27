import { mockPricing } from '../data/mockData';

export const pricingService = {
  getAll: async () => mockPricing,
  getById: async (id) => mockPricing.find((item) => item.id === Number(id)),
};
