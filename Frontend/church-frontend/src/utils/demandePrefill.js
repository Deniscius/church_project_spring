/**
 * Préremplissage du brouillon demande depuis l'accueil / horaires publics.
 * Écrit dans sessionStorage avant navigation vers /demande (hors Provider).
 */
import { findNextAllowedDate, getMinimumCelebrationDateIso } from './schedulingUtils';

export const DRAFT_STORAGE_KEY = 'public_demande_draft_v4';

export function suggestCelebrationDateFromWeekday(jourSemaine, delaiMinimumHeures = 24) {
  if (!jourSemaine) return '';
  const minIso = getMinimumCelebrationDateIso(delaiMinimumHeures);
  return findNextAllowedDate(minIso, [jourSemaine]) || '';
}

export function seedDemandeDraftFromSchedule({
  paroissePublicId,
  paroisseNom,
  horairePublicId,
  horaireLibelle,
  heureCelebration,
  jourSemaine,
  delaiMinimumHeures = 24,
} = {}) {
  if (!paroissePublicId) return;
  try {
    let current = {};
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object') current = parsed;
    }
    const dateDebut = suggestCelebrationDateFromWeekday(jourSemaine, delaiMinimumHeures);
    const next = {
      ...current,
      paroissePublicId,
      paroisseNom: paroisseNom || current.paroisseNom || '',
      // Reset type/forfait when changing parish context from home
      typeDemandePublicId: '',
      typeDemandeLibelle: '',
      forfaitTarifPublicId: '',
      forfaitLabel: '',
      forfaitNature: '',
      forfaitNombreCelebration: null,
      datesCelebration: [],
      dateSchedules: {},
      horairePublicId: horairePublicId || '',
      horaireLibelle: horaireLibelle || '',
      horaireHeureCelebration: heureCelebration || '',
      horaireJourSemaine: jourSemaine || '',
      heurePersonnalisee: '',
      dateDebut: dateDebut || '',
      prefillFromSchedule: Boolean(horairePublicId || jourSemaine),
    };
    sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(next));
  } catch {
    /* ignore quota / private mode */
  }
}

/** URL de demande : les UUID restent en sessionStorage (pas dans la barre d’adresse). */
export function buildDemandePrefillPath() {
  return '/demande';
}

/** Seed + URL — à appeler uniquement sur interaction (click), pas au render. */
export function navigateDemandePrefillPath(params = {}) {
  seedDemandeDraftFromSchedule(params);
  return '/demande';
}
