import { useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { requestService } from '../../services/request.service';

export const parishDemandeKeys = {
  all: (parishId) => ['demandes', 'paroisse', parishId],
  list: (parishId) => [...parishDemandeKeys.all(parishId), 'list'],
  page: (parishId, page, size) => [...parishDemandeKeys.all(parishId), 'page', page, size],
  stats: (parishId) => [...parishDemandeKeys.all(parishId), 'stats'],
};

/** Liste complète DemandeResponse (partagée Dashboard/Paiements si besoin). */
export function useParishDemandes(parishId) {
  return useQuery({
    queryKey: parishDemandeKeys.list(parishId),
    queryFn: ({ signal }) => requestService.getByParish(parishId, { signal }),
    enabled: Boolean(parishId),
    staleTime: 3 * 60_000,
  });
}

/** Liste paginée pour l'écran Demandes. */
export function useParishDemandesPage(parishId, page = 0, size = 20) {
  return useQuery({
    queryKey: parishDemandeKeys.page(parishId, page, size),
    queryFn: ({ signal }) => requestService.getByParish(parishId, { page, size, signal }),
    enabled: Boolean(parishId),
    staleTime: 60_000,
    placeholderData: keepPreviousData,
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
