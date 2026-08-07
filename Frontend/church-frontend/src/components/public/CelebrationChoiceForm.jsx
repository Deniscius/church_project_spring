import React, { useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import { FieldLabel } from '../ui/HelpTip';
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
 * Étape 2 — paroisse, type et tarif dans une seule carte progressive.
 */
export default function CelebrationChoiceForm() {
  const { draft, dispatch } = usePublicDemandeDraft();
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

  const typeOptions = useMemo(
    () => types.map((t) => ({ value: t.publicId, label: t.libelle })),
    [types]
  );

  const forfaitsByNature = useMemo(
    () => Object.fromEntries(forfaits.map((f) => [f.natureForfait, f])),
    [forfaits]
  );

  const natureOptions = useMemo(
    () => NATURE_FORFAIT_OPTIONS
      .filter((option) => forfaitsByNature[option.value])
      .map((option) => {
        const forfait = forfaitsByNature[option.value];
        const amount = formatCurrency(
          forfait.montantForfait != null ? Number(forfait.montantForfait) : 0
        );
        const multi = isMultiCelebrationForfait(forfait.nombreCelebration);
        const duree = multi
          ? ` · ${getForfaitDureeLabel(forfait.nombreCelebration)}`
          : '';
        return {
          value: option.value,
          label: `${option.label} — ${amount}${duree}`,
        };
      }),
    [forfaitsByNature]
  );

  const selectNature = (nature) => {
    const forfait = forfaitsByNature[nature];
    if (!forfait) return;
    const n = forfait.nombreCelebration != null ? Number(forfait.nombreCelebration) : null;
    const multi = isMultiCelebrationForfait(n);
    const natureLabel = NATURE_FORFAIT_OPTIONS.find((o) => o.value === nature)?.label || forfait.nomForfait;
    const dureeLabel = getForfaitDureeLabel(forfait.nombreCelebration);
    dispatch({
      type: 'SELECT_FORFAIT',
      payload: {
        publicId: forfait.publicId,
        label: multi ? `${natureLabel} · ${dureeLabel}` : natureLabel,
        natureForfait: nature,
        heurePersonnalise: forfait.heurePersonnalise,
        nombreCelebration: forfait.nombreCelebration,
        nombreJour: forfait.nombreJour ?? forfait.nombreCelebration,
        montantForfait: forfait.montantForfait,
        joursCelebrationAutorises: forfait.joursCelebrationAutorises || [],
      },
    });
  };

  const typesBusy = loadingTypes || fetchingTypes;
  const forfaitsBusy = loadingForfaits || fetchingForfaits;

  return (
    <AppCard
      title="Où et pour quelle messe ?"
      subtitle="Choisissez la paroisse, puis le type et le tarif — étape par étape."
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
          <FieldLabel htmlFor="public-type-demande" help={HELP.demande.typeDemande} required>
            2. Type de demande
          </FieldLabel>
          {!draft.paroissePublicId ? (
            <p className="muted">Choisissez d’abord la paroisse.</p>
          ) : null}
          {typeError ? <p className="text-red-600">{typeError.message}</p> : null}
          {draft.paroissePublicId && typesBusy ? <p className="muted">Chargement…</p> : null}
          {draft.paroissePublicId && !typesBusy && !typeError && types.length === 0 ? (
            <p className="text-red-600">Aucun type configuré pour cette paroisse.</p>
          ) : null}
          <AppSelect
            id="public-type-demande"
            placeholder="— Choisir un type —"
            value={draft.typeDemandePublicId}
            options={typeOptions}
            disabled={!draft.paroissePublicId || typesBusy}
            required
            onChange={(id) => {
              const t = types.find((x) => x.publicId === id);
              dispatch({
                type: 'SELECT_TYPE_DEMANDE',
                payload: {
                  publicId: id,
                  libelle: t?.libelle || '',
                  delaiMinimumHeures: t?.delaiMinimumHeures ?? 24,
                  joursCelebrationAutorises: t?.joursCelebrationAutorises || [],
                },
              });
            }}
          />
          {draft.typeDemandePublicId ? (
            <small className="muted">
              Délai minimum : {draft.typeDemandeDelaiMinimumHeures} h avant la célébration
              {draft.typeDemandeJoursCelebrationAutorises?.length
                ? ` · ${formatAllowedDays(draft.typeDemandeJoursCelebrationAutorises)}`
                : ''}
              .
            </small>
          ) : null}
        </div>

        <div className="form-field">
          <FieldLabel htmlFor="public-nature" help={HELP.demande.nature} required>
            3. Tarif / nature
          </FieldLabel>
          {!draft.typeDemandePublicId ? (
            <p className="muted">Choisissez d’abord le type de demande.</p>
          ) : null}
          {forfaitError ? <p className="text-red-600">{forfaitError.message}</p> : null}
          {draft.typeDemandePublicId && forfaitsBusy ? <p className="muted">Chargement…</p> : null}
          {draft.typeDemandePublicId && !forfaitsBusy && !natureOptions.length ? (
            <p className="text-red-600">Aucun tarif actif pour ce type.</p>
          ) : null}
          <AppSelect
            id="public-nature"
            placeholder="— Choisir un tarif —"
            value={draft.forfaitNature || ''}
            options={natureOptions}
            disabled={!draft.typeDemandePublicId || forfaitsBusy}
            required
            onChange={selectNature}
          />
          {draft.forfaitMontant != null ? (
            <small className="muted">
              Montant : {formatCurrency(Number(draft.forfaitMontant))}
              {draft.forfaitLabel ? ` · ${draft.forfaitLabel}` : ''}
            </small>
          ) : null}
        </div>
      </div>
    </AppCard>
  );
}
