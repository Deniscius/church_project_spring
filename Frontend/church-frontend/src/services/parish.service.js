import { mockParishes } from '../data/mockData';

export const parishService = {
  getAll: async () => mockParishes,
  getById: async (id) => mockParishes.find((item) => item.id === Number(id)),
  create: async (payload) => payload,
  update: async (id, payload) => ({ id, ...payload }),
};
