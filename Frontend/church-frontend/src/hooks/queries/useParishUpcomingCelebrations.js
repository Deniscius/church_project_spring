import { useQuery, useQueryClient } from '@tanstack/react-query';
import { dashboardService } from '../../services/dashboard.service';

export const parishCelebrationKeys = {
  all: (parishId) => ['dashboard', 'programmations', parishId],
  upcoming: (parishId, jours) => [...parishCelebrationKeys.all(parishId), 'upcoming', jours],
  past: (parishId, jours) => [...parishCelebrationKeys.all(parishId), 'past', jours],
  day: (parishId, date) => [...parishCelebrationKeys.all(parishId), 'day', date],
};

export function useParishUpcomingCelebrations(parishId, jours = 14) {
  return useQuery({
    queryKey: parishCelebrationKeys.upcoming(parishId, jours),
    queryFn: ({ signal }) => dashboardService.getUpcomingCelebrations(parishId, { jours, signal }),
    enabled: Boolean(parishId),
    staleTime: 60_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });
}

export function useParishPastCelebrations(parishId, jours = 14) {
  return useQuery({
    queryKey: parishCelebrationKeys.past(parishId, jours),
    queryFn: ({ signal }) => dashboardService.getPastCelebrations(parishId, { jours, signal }),
    enabled: Boolean(parishId),
    staleTime: 60_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });
}

export function useParishCelebrationsForDay(parishId, date) {
  return useQuery({
    queryKey: parishCelebrationKeys.day(parishId, date),
    queryFn: ({ signal }) => dashboardService.getCelebrationsForDay(parishId, date, { signal }),
    enabled: Boolean(parishId && date),
    staleTime: 30_000,
    select: (data) => (Array.isArray(data) ? data : []),
  });
}

export function useInvalidateParishCelebrations() {
  const queryClient = useQueryClient();
  return (parishId) => queryClient.invalidateQueries({ queryKey: parishCelebrationKeys.all(parishId) });
}
