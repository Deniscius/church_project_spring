import { mockRequests } from '../data/mockData';

export const requestService = {
  getAll: async () => mockRequests,
  getById: async (id) => mockRequests.find((item) => item.id === Number(id)),
  getByTrackingCode: async (code) => mockRequests.find((item) => item.trackingCode === code),
  create: async (payload) => payload,
  update: async (id, payload) => ({ id, ...payload }),
  remove: async (id) => id,
};
