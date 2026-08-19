import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../../services/dashboard.service';

export const parishUpcomingCelebrationKeys = {
  all: (parishId) => ['dashboard', 'programmations', parishId],
  window: (parishId, jours) => [...parishUpcomingCelebrationKeys.all(parishId), jours],
};

export function useParishUpcomingCelebrations(parishId, jours = 14) {
  return useQuery({
    queryKey: parishUpcomingCelebrationKeys.window(parishId, jours),
    queryFn: ({ signal }) => dashboardService.getUpcomingCelebrations(parishId, { jours, signal }),
    enabled: Boolean(parishId),
    staleTime: 60_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });
}
