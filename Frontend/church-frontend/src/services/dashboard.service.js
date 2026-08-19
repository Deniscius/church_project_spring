import { apiClient } from './http/apiClient';

export const dashboardService = {
  getUpcomingCelebrations: (paroissePublicId, { jours = 14, signal } = {}) => {
    const params = new URLSearchParams();
    params.set('jours', String(jours));
    return apiClient(
      `/dashboard/paroisses/${paroissePublicId}/programmations?${params.toString()}`,
      {},
      { auth: true, signal }
    );
  },
};
