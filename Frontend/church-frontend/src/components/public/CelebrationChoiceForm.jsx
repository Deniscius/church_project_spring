import React, { useMemo, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import AppButton from '../ui/AppButton';
import { FieldLabel } from '../ui/HelpTip';
import FormuleChoiceModal from './FormuleChoiceModal';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import {
  useForfaitsActifsQuery,
  useParoissesPublicQuery,
  useTypeDemandesByParishQuery,
} from '../../hooks/queries/usePublicReferentiel';
import { getForfaitDureeLabel, isMultiCelebrationForfait, NATURE_FORFAIT_OPTIONS } from '../../constants/enums';
import { formatAllowedDays } from '../../utils/schedulingUtils';
import { formatCurrency } from '../../utils/formatCurrency';
import { HELP } from '../../constants/helpTips';

/**
 * Étape 2 — paroisse + modal de formule (triduum / neuvaine / tarif).
 */
export default function CelebrationChoiceForm() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const [formuleOpen, setFormuleOpen] = useState(false);
  const { data: paroisses = [], isLoading: loadingParishes, error: parishError } = useParoissesPublicQuery();
  const {
    data: types = [],
    isLoading: loadingTypes,
    isFetching: fetchingTypes,
    error: typeError,
  } = useTypeDemandesByParishQuery(draft.paroissePublicId);
  const {
    data: forfaits = [],
    isLoading: loadingForfaits,
    isFetching: fetchingForfaits,
    error: forfaitError,
  } = useForfaitsActifsQuery(draft.typeDemandePublicId);

  const parishOptions = useMemo(
    () => paroisses.map((p) => ({
      value: p.publicId,
      label: p.doyenneNom ? `${p.nom} (${p.doyenneNom})` : p.nom,
    })),
    [paroisses]
  );

  const typesBusy = loadingTypes || fetchingTypes;
  const forfaitsBusy = loadingForfaits || fetchingForfaits;

  const selectType = (t) => {
    if (!t) return;
    dispatch({
      type: 'SELECT_TYPE_DEMANDE',
      payload: {
        publicId: t.publicId,
        libelle: t.libelle || '',
        delaiMinimumHeures: t.delaiMinimumHeures ?? 24,
        joursCelebrationAutorises: t.joursCelebrationAutorises || [],
      },
    });
  };

  const selectForfait = (forfait) => {
    if (!forfait) return;
    const n = forfait.nombreCelebration != null ? Number(forfait.nombreCelebration) : null;
    const multi = isMultiCelebrationForfait(n);
    const natureLabel = NATURE_FORFAIT_OPTIONS.find((o) => o.value === forfait.natureForfait)?.label
      || forfait.nomForfait;
    const dureeLabel = getForfaitDureeLabel(forfait.nombreCelebration);
    dispatch({
      type: 'SELECT_FORFAIT',
      payload: {
        publicId: forfait.publicId,
        label: multi ? `${natureLabel} · ${dureeLabel}` : natureLabel,
        natureForfait: forfait.natureForfait,
        heurePersonnalise: forfait.heurePersonnalise,
        nombreCelebration: forfait.nombreCelebration,
        nombreJour: forfait.nombreJour ?? forfait.nombreCelebration,
        montantForfait: forfait.montantForfait,
        joursCelebrationAutorises: forfait.joursCelebrationAutorises || [],
      },
    });
  };

  const summary = [
    draft.typeDemandeLibelle,
    draft.forfaitLabel,
    draft.forfaitMontant != null ? formatCurrency(Number(draft.forfaitMontant)) : null,
  ].filter(Boolean).join(' · ');

  return (
    <AppCard
      title="Où et pour quelle messe ?"
      subtitle="Choisissez la paroisse, puis la formule (messe, triduum, neuvaine…)."
    >
      <div className="stack demande-choice-steps" style={{ gap: 18 }}>
        <div className="form-field">
          <FieldLabel htmlFor="public-parish" help={HELP.demande.paroisse} required>
            1. Paroisse
          </FieldLabel>
          {parishError ? <p className="text-red-600">{parishError.message}</p> : null}
          {loadingParishes ? <p className="muted">Chargement des paroisses…</p> : null}
          <AppSelect
            id="public-parish"
            searchable
            searchPlaceholder="Tapez le nom…"
            placeholder="— Choisir votre paroisse —"
            value={draft.paroissePublicId}
            options={parishOptions}
            disabled={loadingParishes}
            required
            onChange={(id) => {
              const p = paroisses.find((x) => x.publicId === id);
              dispatch({
                type: 'SELECT_PAROISSE',
                payload: { publicId: id, nom: p?.nom || '' },
              });
            }}
          />
        </div>

        <div className="form-field">
          <FieldLabel help={HELP.demande.typeDemande} required>
            2. Formule de célébration
          </FieldLabel>
          {!draft.paroissePublicId ? (
            <p className="muted">Choisissez d’abord la paroisse.</p>
          ) : null}
          {typeError ? <p className="text-red-600">{typeError.message}</p> : null}
          {forfaitError ? <p className="text-red-600">{forfaitError.message}</p> : null}

          {draft.paroissePublicId ? (
            <div className="formule-summary-box">
              {summary ? (
                <>
                  <p className="formule-summary-value">{summary}</p>
                  {draft.typeDemandePublicId ? (
                    <small className="muted">
                      Délai minimum : {draft.typeDemandeDelaiMinimumHeures} h
                      {draft.typeDemandeJoursCelebrationAutorises?.length
                        ? ` · ${formatAllowedDays(draft.typeDemandeJoursCelebrationAutorises)}`
                        : ''}
                      {isMultiCelebrationForfait(draft.forfaitNombreCelebration)
                        ? ` · ${getForfaitDureeLabel(draft.forfaitNombreCelebration)}`
                        : ''}
                    </small>
                  ) : null}
                </>
              ) : (
                <p className="muted" style={{ margin: 0 }}>
                  Triduum, neuvaine, trentaine ou messe unique — choisissez dans le panneau.
                </p>
              )}
              <AppButton
                type="button"
                variant={summary ? 'secondary' : 'primary'}
                disabled={typesBusy}
                onClick={() => setFormuleOpen(true)}
              >
                {typesBusy
                  ? 'Chargement…'
                  : summary
                    ? 'Modifier la formule'
                    : 'Choisir triduum, neuvaine…'}
              </AppButton>
            </div>
          ) : null}
        </div>
      </div>

      <FormuleChoiceModal
        open={formuleOpen}
        onClose={() => setFormuleOpen(false)}
        types={types}
        typesLoading={typesBusy}
        forfaits={forfaits}
        forfaitsLoading={forfaitsBusy}
        selectedTypeId={draft.typeDemandePublicId}
        selectedNature={draft.forfaitNature}
        onSelectType={selectType}
        onSelectForfait={selectForfait}
      />
    </AppCard>
  );
}
