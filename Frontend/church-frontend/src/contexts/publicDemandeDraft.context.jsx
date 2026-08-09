/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
} from 'react';
import { toE164, DEFAULT_PHONE_COUNTRY_ISO, parseStoredPhone } from '../utils/phone';
import { DRAFT_STORAGE_KEY, suggestCelebrationDateFromWeekday } from '../utils/demandePrefill';
import {
  findNextAllowedDate,
  getDayEnumFromDateString,
  getEffectiveAllowedDays,
  getMinimumCelebrationDateIso,
} from '../utils/schedulingUtils';

const initialDraft = {
  intention: '',
  prenomFidele: '',
  nomFidele: '',
  telFidele: '',
  /** ISO pays pour l'indicatif (ex. TG) — le national est saisi à part. */
  telCountryIso: DEFAULT_PHONE_COUNTRY_ISO,
  telNational: '',
  emailFidele: '',
  nomCoursier: '',
  paroissePublicId: '',
  paroisseNom: '',
  typeDemandePublicId: '',
  typeDemandeLibelle: '',
  typeDemandeDelaiMinimumHeures: 24,
  typeDemandeJoursCelebrationAutorises: [],
  forfaitTarifPublicId: '',
  forfaitLabel: '',
  forfaitNature: '',
  forfaitHeurePersonnalise: false,
  forfaitNombreCelebration: null,
  forfaitNombreJour: null,
  forfaitMontant: null,
  forfaitJoursCelebrationAutorises: [],
  horairePublicId: '',
  horaireLibelle: '',
  horaireHeureCelebration: '',
  horaireJourSemaine: '',
  heurePersonnalisee: '',
  dateDebut: '',
  datesCelebration: [],
  /** Multi : { [isoDate]: { horairePublicId, horaireLibelle, heureCelebration, jourSemaine, heurePersonnalisee } } */
  dateSchedules: {},
  typePaiementPublicId: '',
  typePaiementLibelle: '',
  /** true si brouillon issu d'un créneau accueil / horaires */
  prefillFromSchedule: false,
  /** Nature d'honoraire du créneau (NORMALE / DOMINICALE / SPECIALE) */
  prefillNatureHonoraire: '',
};

function loadDraftFromStorage() {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) return { ...initialDraft };
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return { ...initialDraft };
    const merged = {
      ...initialDraft,
      ...parsed,
      datesCelebration: Array.isArray(parsed.datesCelebration) ? parsed.datesCelebration : [],
      dateSchedules:
        parsed.dateSchedules && typeof parsed.dateSchedules === 'object'
          ? parsed.dateSchedules
          : {},
    };
    if (merged.telNational) {
      const iso = merged.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO;
      merged.telCountryIso = iso;
      merged.telFidele = toE164(iso, merged.telNational) || merged.telFidele || '';
    } else if (merged.telFidele) {
      const parsedPhone = parseStoredPhone(
        merged.telFidele,
        merged.telCountryIso || DEFAULT_PHONE_COUNTRY_ISO
      );
      merged.telCountryIso = parsedPhone.iso;
      merged.telNational = parsedPhone.national;
      merged.telFidele = toE164(parsedPhone.iso, parsedPhone.national) || merged.telFidele;
    }
    return merged;
  } catch {
    return { ...initialDraft };
  }
}

function clearScheduleFields(state) {
  return {
    ...state,
    horairePublicId: '',
    horaireLibelle: '',
    horaireHeureCelebration: '',
    horaireJourSemaine: '',
    heurePersonnalisee: '',
    dateDebut: '',
    datesCelebration: [],
    dateSchedules: {},
    prefillFromSchedule: false,
    prefillNatureHonoraire: '',
  };
}

/** Conserve le créneau (et date) issu de l'accueil lors du choix type/forfait. */
function withPreservedSchedulePrefill(previous, next) {
  if (!previous.horairePublicId && !previous.prefillFromSchedule) {
    return next;
  }
  return {
    ...next,
    horairePublicId: previous.horairePublicId || next.horairePublicId,
    horaireLibelle: previous.horaireLibelle || next.horaireLibelle,
    horaireHeureCelebration: previous.horaireHeureCelebration || next.horaireHeureCelebration,
    horaireJourSemaine: previous.horaireJourSemaine || next.horaireJourSemaine,
    dateDebut: previous.dateDebut || next.dateDebut,
    prefillFromSchedule: previous.prefillFromSchedule || next.prefillFromSchedule,
    prefillNatureHonoraire: previous.prefillNatureHonoraire || next.prefillNatureHonoraire,
  };
}

function applySuggestedDate(state) {
  if (!state.horaireJourSemaine || !state.forfaitTarifPublicId) return state;
  const n = state.forfaitNombreCelebration != null ? Number(state.forfaitNombreCelebration) : null;
  if (n != null && n > 1) return state;

  const allowed = getEffectiveAllowedDays(
    state.typeDemandeJoursCelebrationAutorises,
    state.forfaitJoursCelebrationAutorises,
    state.horaireJourSemaine
  );
  if (!allowed.length) return state;
  const minIso = getMinimumCelebrationDateIso(state.typeDemandeDelaiMinimumHeures);

  // Préremplissage : conserver la date déjà calculée si elle reste valide.
  if (state.prefillFromSchedule && state.dateDebut && state.dateDebut >= minIso) {
    const day = getDayEnumFromDateString(state.dateDebut);
    if (day && allowed.includes(day)) {
      return { ...state, datesCelebration: [] };
    }
  }

  const dateDebut = findNextAllowedDate(minIso, allowed)
    || suggestCelebrationDateFromWeekday(state.horaireJourSemaine, state.typeDemandeDelaiMinimumHeures);
  if (!dateDebut) return state;
  return { ...state, dateDebut, datesCelebration: [] };
}

function draftReducer(state, action) {
  switch (action.type) {
    case 'PATCH':
      return { ...state, ...action.payload };
    case 'SELECT_PAROISSE': {
      const { publicId, nom } = action.payload;
      // Même paroisse : ne pas effacer le préremplissage horaire/date.
      if (publicId && publicId === state.paroissePublicId) {
        return {
          ...state,
          paroisseNom: nom || state.paroisseNom || '',
        };
      }
      // Créneau figé : la paroisse ne peut pas être changée.
      if (state.prefillFromSchedule && state.paroissePublicId) {
        return state;
      }
      return clearScheduleFields({
        ...state,
        paroissePublicId: publicId,
        paroisseNom: nom || '',
        typeDemandePublicId: '',
        typeDemandeLibelle: '',
        typeDemandeDelaiMinimumHeures: 24,
        typeDemandeJoursCelebrationAutorises: [],
        forfaitTarifPublicId: '',
        forfaitLabel: '',
        forfaitNature: '',
        forfaitHeurePersonnalise: false,
        forfaitNombreCelebration: null,
        forfaitNombreJour: null,
        forfaitMontant: null,
        forfaitJoursCelebrationAutorises: [],
      });
    }
    case 'SELECT_TYPE_DEMANDE': {
      const { publicId, libelle, delaiMinimumHeures, joursCelebrationAutorises } = action.payload;
      // Créneau figé : seul le 1er type (messe unique) peut être posé, pas un changement manuel.
      if (
        state.prefillFromSchedule
        && state.typeDemandePublicId
        && publicId
        && publicId !== state.typeDemandePublicId
      ) {
        return state;
      }
      const cleared = clearScheduleFields({
        ...state,
        typeDemandePublicId: publicId,
        typeDemandeLibelle: libelle || '',
        typeDemandeDelaiMinimumHeures: delaiMinimumHeures ?? 24,
        typeDemandeJoursCelebrationAutorises: joursCelebrationAutorises || [],
        forfaitTarifPublicId: '',
        forfaitLabel: '',
        forfaitNature: '',
        forfaitHeurePersonnalise: false,
        forfaitNombreCelebration: null,
        forfaitNombreJour: null,
        forfaitMontant: null,
        forfaitJoursCelebrationAutorises: [],
      });
      return withPreservedSchedulePrefill(state, cleared);
    }
    case 'SELECT_FORFAIT': {
      const {
        publicId,
        label,
        natureForfait,
        heurePersonnalise,
        nombreCelebration,
        nombreJour,
        montantForfait,
        joursCelebrationAutorises,
      } = action.payload;
      const n = nombreCelebration != null ? Number(nombreCelebration) : null;
      // Créneau figé : interdire triduum / neuvaine / autre formule multi.
      if (state.prefillFromSchedule && n != null && n > 1) {
        return state;
      }
      const next = clearScheduleFields({
        ...state,
        forfaitTarifPublicId: publicId,
        forfaitLabel: label || '',
        forfaitNature: natureForfait || '',
        forfaitHeurePersonnalise: Boolean(heurePersonnalise),
        forfaitNombreCelebration: n,
        forfaitNombreJour: nombreJour != null ? Number(nombreJour) : n,
        forfaitMontant: montantForfait != null ? Number(montantForfait) : null,
        forfaitJoursCelebrationAutorises: joursCelebrationAutorises || [],
        datesCelebration: n != null && n > 1 ? Array.from({ length: n }, () => '') : [],
      });
      // Préremplissage accueil : conserver créneau + recalculer la prochaine date.
      if (n === 1 || n == null) {
        return applySuggestedDate(withPreservedSchedulePrefill(state, next));
      }
      return next;
    }
    /** Met à jour le tarif selon le jour sans effacer date / horaire. */
    case 'SYNC_FORFAIT': {
      const {
        publicId,
        label,
        natureForfait,
        heurePersonnalise,
        nombreCelebration,
        nombreJour,
        montantForfait,
        joursCelebrationAutorises,
      } = action.payload;
      if (!publicId || publicId === state.forfaitTarifPublicId) return state;
      const n = nombreCelebration != null ? Number(nombreCelebration) : state.forfaitNombreCelebration;
      return {
        ...state,
        forfaitTarifPublicId: publicId,
        forfaitLabel: label || state.forfaitLabel || '',
        forfaitNature: natureForfait || '',
        forfaitHeurePersonnalise: heurePersonnalise != null
          ? Boolean(heurePersonnalise)
          : state.forfaitHeurePersonnalise,
        forfaitNombreCelebration: n,
        forfaitNombreJour: nombreJour != null ? Number(nombreJour) : state.forfaitNombreJour,
        forfaitMontant: montantForfait != null ? Number(montantForfait) : null,
        forfaitJoursCelebrationAutorises: joursCelebrationAutorises || [],
      };
    }
    case 'SELECT_HORAIRE': {
      const { publicId, libelle, heureCelebration, jourSemaine, preserveDate } = action.payload;
      const next = {
        ...state,
        horairePublicId: publicId,
        horaireLibelle: libelle || '',
        horaireHeureCelebration: heureCelebration || '',
        horaireJourSemaine: jourSemaine || '',
        prefillFromSchedule: Boolean(preserveDate && state.prefillFromSchedule && publicId),
      };
      if (!publicId) {
        next.prefillFromSchedule = false;
        next.prefillNatureHonoraire = '';
      }
      // Sans preserveDate : nouveau choix manuel → date à resaisir.
      if (!preserveDate) {
        next.dateDebut = '';
        next.datesCelebration = [];
        next.prefillFromSchedule = false;
      }
      return next;
    }
    case 'SELECT_PAIEMENT': {
      const { publicId, libelle } = action.payload;
      return {
        ...state,
        typePaiementPublicId: publicId,
        typePaiementLibelle: libelle || '',
      };
    }
    case 'RESET':
      return { ...initialDraft };
    default:
      return state;
  }
}

export const PublicDemandeDraftContext = createContext(null);

export function PublicDemandeDraftProvider({ children }) {
  const [draft, dispatch] = useReducer(draftReducer, undefined, loadDraftFromStorage);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
      } catch {
        /* quota */
      }
    }, 400);
    return () => window.clearTimeout(timer);
  }, [draft]);

  const patch = useCallback((payload) => {
    dispatch({ type: 'PATCH', payload });
  }, []);

  const reset = useCallback(() => {
    try {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      sessionStorage.removeItem('public_demande_draft_v1');
    } catch {
      /* */
    }
    dispatch({ type: 'RESET' });
  }, []);

  const value = useMemo(
    () => ({
      draft,
      dispatch,
      patch,
      reset,
    }),
    [draft, patch, reset]
  );

  return (
    <PublicDemandeDraftContext.Provider value={value}>
      {children}
    </PublicDemandeDraftContext.Provider>
  );
}

export function usePublicDemandeDraft() {
  const ctx = useContext(PublicDemandeDraftContext);
  if (!ctx) {
    throw new Error('usePublicDemandeDraft doit être utilisé dans PublicDemandeDraftProvider');
  }
  return ctx;
}
