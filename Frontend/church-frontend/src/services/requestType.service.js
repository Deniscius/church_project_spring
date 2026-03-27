import { mockRequestTypes } from '../data/mockData';

export const requestTypeService = {
  getAll: async () => mockRequestTypes,
  getById: async (id) => mockRequestTypes.find((item) => item.id === Number(id)),
};
