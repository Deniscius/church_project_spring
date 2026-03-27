import { mockUsers } from '../data/mockData';

export const userService = {
  getAll: async () => mockUsers,
  getById: async (id) => mockUsers.find((item) => item.id === Number(id)),
};
