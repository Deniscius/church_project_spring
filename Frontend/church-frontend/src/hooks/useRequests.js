import { mockRequests } from '../data/mockData';

export function useRequests() {
  return {
    data: mockRequests,
    isLoading: false,
    error: null,
  };
}
