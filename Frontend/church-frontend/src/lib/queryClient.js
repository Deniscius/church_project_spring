import { QueryClient } from '@tanstack/react-query';

/**
 * Client React Query orienté charge :
 * - staleTime élevé sur référentiels
 * - pas de refetch au focus (évite les pics inutiles)
 * - retry limité
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60_000,
      gcTime: 30 * 60_000,
      retry: 1,
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 8000),
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
      networkMode: 'online',
      structuralSharing: true,
    },
    mutations: {
      retry: 0,
      networkMode: 'online',
    },
  },
});
