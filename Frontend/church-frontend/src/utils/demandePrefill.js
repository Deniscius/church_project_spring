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

/**
 * @param {object} params
 * @param {string} [params.paroissePublicId]
 * @param {string} [params.paroisseNom]
 * @param {string} [params.horairePublicId]
 * @param {string} [params.horaireLibelle]
 * @param {string} [params.heureCelebration]
 * @param {string} [params.jourSemaine]
 * @param {string} [params.natureHonoraire] NORMALE | DOMINICALE | SPECIALE
 * @param {number} [params.delaiMinimumHeures]
 */
export function seedDemandeDraftFromSchedule({
  paroissePublicId,
  paroisseNom,
  horairePublicId,
  horaireLibelle,
  heureCelebration,
  jourSemaine,
  natureHonoraire,
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
      // Nouveau créneau : on repart sur une formule compatible (auto-appliquée ensuite).
      typeDemandePublicId: '',
      typeDemandeLibelle: '',
      typeDemandeDelaiMinimumHeures: delaiMinimumHeures ?? 24,
      typeDemandeJoursCelebrationAutorises: [],
      forfaitTarifPublicId: '',
      forfaitLabel: '',
      forfaitNature: '',
      forfaitHeurePersonnalise: false,
      forfaitNombreCelebration: null,
      forfaitNombreJour: null,
      forfaitMontant: null,
      forfaitJoursCelebrationAutorises: [],
      datesCelebration: [],
      dateSchedules: {},
      horairePublicId: horairePublicId || '',
      horaireLibelle: horaireLibelle || '',
      horaireHeureCelebration: heureCelebration || '',
      horaireJourSemaine: jourSemaine || '',
      heurePersonnalisee: '',
      dateDebut: dateDebut || '',
      prefillNatureHonoraire: natureHonoraire || '',
      prefillFromSchedule: Boolean(horairePublicId || jourSemaine || natureHonoraire),
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

/** Seed + chemin — à appeler uniquement sur interaction (click), pas au render. */
export function navigateDemandePrefillPath(params = {}) {
  seedDemandeDraftFromSchedule(params);
  return '/demande';
}

/**
 * Choisit un type « messe unique » plutôt qu’un triduum/neuvaine/trentaine.
 */
export function pickPreferredTypeDemande(types = []) {
  if (!types?.length) return null;
  const scored = types.map((t) => {
    const lib = String(t.libelle || '').toLowerCase();
    let score = 0;
    if (lib.includes('trentaine')) score -= 30;
    else if (lib.includes('neuvaine')) score -= 20;
    else if (lib.includes('triduum')) score -= 10;
    if (lib.includes('messe') || lib.includes('intention') || lib.includes('eucharist')) score += 5;
    return { t, score };
  });
  scored.sort((a, b) => b.score - a.score);
  return scored[0]?.t || types[0];
}

/**
 * Nature de tarif attendue pour un jour / créneau de célébration.
 * SPECIALE explicite (choix fidèle) est conservée sauf créneau imposé autrement.
 */
export function resolveNatureForCelebrationDay({
  jourSemaine,
  natureHonoraire,
  currentNature,
} = {}) {
  if (natureHonoraire === 'SPECIALE' || natureHonoraire === 'DOMINICALE' || natureHonoraire === 'NORMALE') {
    return natureHonoraire;
  }
  if (currentNature === 'SPECIALE') return 'SPECIALE';
  if (jourSemaine === 'DIMANCHE') return 'DOMINICALE';
  if (jourSemaine) return 'NORMALE';
  return currentNature || 'NORMALE';
}

/**
 * Choisit le forfait (nature) le plus cohérent avec le créneau / jour cliqué.
 */
export function pickPreferredForfait(forfaits = [], {
  natureHonoraire,
  jourSemaine,
  currentNature,
  nombreCelebration,
} = {}) {
  if (!forfaits?.length) return null;

  const targetN = nombreCelebration != null ? Number(nombreCelebration) : null;
  const pool = forfaits.filter((f) => {
    const n = f.nombreCelebration != null ? Number(f.nombreCelebration) : 1;
    if (targetN != null && Number.isFinite(targetN)) {
      return n === targetN;
    }
    return !Number.isFinite(n) || n <= 1;
  });
  const list = pool.length ? pool : forfaits;

  const nature = resolveNatureForCelebrationDay({
    jourSemaine,
    natureHonoraire,
    currentNature,
  });
  const byNature = (value) => list.find((f) => f.natureForfait === value);
  if (nature && byNature(nature)) return byNature(nature);
  if (jourSemaine === 'DIMANCHE' && byNature('DOMINICALE')) return byNature('DOMINICALE');
  if (byNature('NORMALE')) return byNature('NORMALE');
  return list[0];
}
