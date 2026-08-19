import { apiClient } from './http/apiClient';

function windowQuery(jours) {
  const params = new URLSearchParams();
  params.set('jours', String(jours));
  return params.toString();
}

export const dashboardService = {
  getUpcomingCelebrations: (paroissePublicId, { jours = 14, signal } = {}) =>
    apiClient(
      `/dashboard/paroisses/${paroissePublicId}/programmations?${windowQuery(jours)}`,
      {},
      { auth: true, signal }
    ),

  getPastCelebrations: (paroissePublicId, { jours = 14, signal } = {}) =>
    apiClient(
      `/dashboard/paroisses/${paroissePublicId}/programmations/passees?${windowQuery(jours)}`,
      {},
      { auth: true, signal }
    ),

  getCelebrationsForDay: (paroissePublicId, date, { signal } = {}) =>
    apiClient(
      `/dashboard/paroisses/${paroissePublicId}/programmations/jour/${encodeURIComponent(date)}`,
      {},
      { auth: true, signal }
    ),

  updateCelebrationSchedule: (paroissePublicId, demandeDatePublicId, horairePublicId) =>
    apiClient(
      `/dashboard/paroisses/${paroissePublicId}/programmations/${demandeDatePublicId}/horaire`,
      {
        method: 'PATCH',
        body: JSON.stringify({ horairePublicId }),
      },
      { auth: true }
    ),
};
