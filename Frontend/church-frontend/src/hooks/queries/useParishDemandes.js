import { useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { requestService } from '../../services/request.service';

export const parishDemandeKeys = {
  all: (parishId) => ['demandes', 'paroisse', parishId],
  page: (parishId, page, size, includeDeleted = false, search = '') => [
    ...parishDemandeKeys.all(parishId),
    'page',
    page,
    size,
    includeDeleted ? 'with-deleted' : 'active',
    search || 'no-search',
  ],
  unpaid: (parishId, page, size) => [...parishDemandeKeys.all(parishId), 'unpaid', page, size],
  stats: (parishId) => [...parishDemandeKeys.all(parishId), 'stats'],
};

/** Liste paginée — recherche serveur si un terme est fourni. */
export function useParishDemandesPage(
  parishId,
  page = 0,
  size = 20,
  includeDeleted = false,
  search = ''
) {
  const normalizedSearch = search.trim();

  return useQuery({
    queryKey: parishDemandeKeys.page(
      parishId,
      page,
      size,
      includeDeleted,
      normalizedSearch
    ),
    queryFn: ({ signal }) => normalizedSearch
      ? requestService.searchByParish(parishId, {
          query: normalizedSearch,
          page,
          size,
          includeDeleted,
          signal,
        })
      : requestService.getByParish(parishId, {
          page,
          size,
          includeDeleted,
          signal,
        }),
    enabled: Boolean(parishId),
    staleTime: 60_000,
    placeholderData: keepPreviousData,
  });
}

export function useParishUnpaidApproaching(parishId, page = 0, size = 10) {
  return useQuery({
    queryKey: parishDemandeKeys.unpaid(parishId, page, size),
    queryFn: ({ signal }) => requestService.getUnpaidApproaching(parishId, { page, size, signal }),
    enabled: Boolean(parishId),
    staleTime: 20_000,
  });
}

export function useParishDemandeStats(parishId) {
  return useQuery({
    queryKey: parishDemandeKeys.stats(parishId),
    queryFn: ({ signal }) => requestService.getParishStats(parishId, { signal }),
    enabled: Boolean(parishId),
    staleTime: 2 * 60_000,
  });
}

export function useInvalidateParishDemandes() {
  const queryClient = useQueryClient();
  return (parishId) => queryClient.invalidateQueries({ queryKey: parishDemandeKeys.all(parishId) });
}
