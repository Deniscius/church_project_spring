import { mockSchedules } from '../data/mockData';

export const scheduleService = {
  getAll: async () => mockSchedules,
  getById: async (id) => mockSchedules.find((item) => item.id === Number(id)),
};
