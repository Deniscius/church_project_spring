import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
} from 'react';

const DRAFT_STORAGE_KEY = 'public_demande_draft_v1';

const initialDraft = {
  intention: '',
  prenomFidele: '',
  nomFidele: '',
  telFidele: '',
  emailFidele: '',
  nomCoursier: '',
  paroissePublicId: '',
  paroisseNom: '',
  typeDemandePublicId: '',
  typeDemandeLibelle: '',
  forfaitTarifPublicId: '',
  forfaitLabel: '',
  forfaitHeurePersonnalise: false,
  forfaitNombreCelebration: null,
  forfaitMontant: null,
  horairePublicId: '',
  horaireLibelle: '',
  heurePersonnalisee: '',
  dateDebut: '',
  typePaiementPublicId: '',
  typePaiementLibelle: '',
};

function loadDraftFromStorage() {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    if (!raw) return { ...initialDraft };
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return { ...initialDraft };
    return { ...initialDraft, ...parsed };
  } catch {
    return { ...initialDraft };
  }
}

function draftReducer(state, action) {
  switch (action.type) {
    case 'PATCH':
      return { ...state, ...action.payload };
    case 'SELECT_PAROISSE': {
      const { publicId, nom } = action.payload;
      return {
        ...state,
        paroissePublicId: publicId,
        paroisseNom: nom || '',
        typeDemandePublicId: '',
        typeDemandeLibelle: '',
        forfaitTarifPublicId: '',
        forfaitLabel: '',
        forfaitHeurePersonnalise: false,
        forfaitNombreCelebration: null,
        forfaitMontant: null,
        horairePublicId: '',
        horaireLibelle: '',
        heurePersonnalisee: '',
      };
    }
    case 'SELECT_TYPE_DEMANDE': {
      const { publicId, libelle } = action.payload;
      return {
        ...state,
        typeDemandePublicId: publicId,
        typeDemandeLibelle: libelle || '',
        forfaitTarifPublicId: '',
        forfaitLabel: '',
        forfaitHeurePersonnalise: false,
        forfaitNombreCelebration: null,
        forfaitMontant: null,
        horairePublicId: '',
        horaireLibelle: '',
        heurePersonnalisee: '',
      };
    }
    case 'SELECT_FORFAIT': {
      const {
        publicId,
        label,
        heurePersonnalise,
        nombreCelebration,
        montantForfait,
      } = action.payload;
      return {
        ...state,
        forfaitTarifPublicId: publicId,
        forfaitLabel: label || '',
        forfaitHeurePersonnalise: Boolean(heurePersonnalise),
        forfaitNombreCelebration:
          nombreCelebration != null ? Number(nombreCelebration) : null,
        forfaitMontant: montantForfait != null ? Number(montantForfait) : null,
        horairePublicId: '',
        horaireLibelle: '',
        heurePersonnalisee: '',
      };
    }
    case 'SELECT_HORAIRE': {
      const { publicId, libelle } = action.payload;
      return {
        ...state,
        horairePublicId: publicId,
        horaireLibelle: libelle || '',
      };
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

const PublicDemandeDraftContext = createContext(null);

export function PublicDemandeDraftProvider({ children }) {
  const [draft, dispatch] = useReducer(draftReducer, undefined, loadDraftFromStorage);

  useEffect(() => {
    try {
      sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
    } catch {
      /* quota */
    }
  }, [draft]);

  const patch = useCallback((payload) => {
    dispatch({ type: 'PATCH', payload });
  }, []);

  const reset = useCallback(() => {
    try {
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
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
