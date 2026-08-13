import { useQuery } from '@tanstack/react-query';
import { parishService } from '../../services/parish.service';
import { requestTypeService } from '../../services/requestType.service';
import { pricingService } from '../../services/pricing.service';
import { scheduleService } from '../../services/schedule.service';
import { paymentTypeService } from '../../services/paymentType.service';

export const qk = {
  paroissesPublic: ['paroisses', 'public'],
  typesParish: (id) => ['type-demandes', 'parish', id],
  forfaitsActifs: (typeId) => ['forfait-tarifs', 'actifs', typeId],
  horairesParish: (id) => ['horaires', 'parish', id],
  horairesPublicActives: ['horaires', 'public', 'paroisses-actives'],
  typePaiementsPublic: ['type-paiement', 'public'],
};

/** Accueil / page horaires : programme de la semaine courante. */
export function useHorairesPublicActivesQuery(options = {}) {
  return useQuery({
    queryKey: qk.horairesPublicActives,
    queryFn: () => scheduleService.listPublicForActiveParishes(),
    // Court : un nouvel horaire admin doit apparaître rapidement.
    staleTime: 60_000,
    refetchOnWindowFocus: true,
    enabled: options.enabled !== false,
  });
}

export function useParoissesPublicQuery(options = {}) {
  return useQuery({
    queryKey: qk.paroissesPublic,
    queryFn: () => parishService.getAllPublic(),
    staleTime: 15 * 60_000,
    enabled: options.enabled !== false,
    select: (data) => (data || []).filter((p) => p.isActive !== false),
  });
}

export function useTypeDemandesByParishQuery(paroissePublicId) {
  return useQuery({
    queryKey: qk.typesParish(paroissePublicId),
    queryFn: () => requestTypeService.getByParishPublic(paroissePublicId),
    enabled: Boolean(paroissePublicId),
    staleTime: 10 * 60_000,
    select: (data) => (data || []).filter((t) => t.isActive !== false && !t.statusDel),
  });
}

export function useForfaitsActifsQuery(typeDemandePublicId) {
  return useQuery({
    queryKey: qk.forfaitsActifs(typeDemandePublicId),
    queryFn: () => pricingService.getActiveByTypeDemandePublic(typeDemandePublicId),
    enabled: Boolean(typeDemandePublicId),
    staleTime: 10 * 60_000,
    select: (data) => (data || []).filter((f) => f.isActive !== false && !f.statusDel),
  });
}

export function useHorairesByParishQuery(paroissePublicId) {
  return useQuery({
    queryKey: qk.horairesParish(paroissePublicId),
    queryFn: () => scheduleService.getByParishPublic(paroissePublicId),
    enabled: Boolean(paroissePublicId),
    staleTime: 10 * 60_000,
    select: (data) => (data || []).filter((h) => h.isActive !== false && !h.statusDel),
  });
}

export function useTypePaiementsPublicQuery(options = {}) {
  return useQuery({
    queryKey: qk.typePaiementsPublic,
    queryFn: () => paymentTypeService.getAllPublic(),
    staleTime: 30 * 60_000,
    enabled: options.enabled !== false,
  });
}
