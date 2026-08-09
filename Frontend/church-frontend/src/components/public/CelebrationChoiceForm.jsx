import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
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
import { getForfaitDureeLabel, isMultiCelebrationForfait, NATURE_FORFAIT_OPTIONS, WEEK_DAY_LABELS } from '../../constants/enums';
import { formatAllowedDays } from '../../utils/schedulingUtils';
import { formatCurrency } from '../../utils/formatCurrency';
import { HELP } from '../../constants/helpTips';
import {
  pickPreferredForfait,
  pickPreferredTypeDemande,
} from '../../utils/demandePrefill';
import { formatDateShort } from '../../utils/formatDate';

/**
 * Étape 2 — paroisse + modal de formule (triduum / neuvaine / tarif).
 * Créneau repris des horaires : paroisse + formule figées (messe unique du jour).
 */
export default function CelebrationChoiceForm() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const [formuleOpen, setFormuleOpen] = useState(false);
  const autoAppliedKey = useRef('');
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

  const lockedFromSchedule = Boolean(
    draft.prefillFromSchedule && (draft.horairePublicId || draft.horaireJourSemaine)
  );

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
    if (!t || lockedFromSchedule) return;
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
    // Depuis un créneau : seul le forfait aligné (SYNC / auto) est autorisé.
    if (lockedFromSchedule && draft.forfaitTarifPublicId && forfait.publicId !== draft.forfaitTarifPublicId) {
      return;
    }
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

  // Auto-applique type « messe unique » + forfait du jour après clic sur un créneau.
  useEffect(() => {
    if (!draft.prefillFromSchedule || !draft.paroissePublicId) return;
    if (typesBusy || !types.length) return;

    const key = [
      draft.paroissePublicId,
      draft.horairePublicId || '',
      draft.prefillNatureHonoraire || '',
      draft.horaireJourSemaine || '',
    ].join('|');

    if (autoAppliedKey.current === key) return;

    if (!draft.typeDemandePublicId) {
      const preferred = pickPreferredTypeDemande(types);
      if (preferred) {
        dispatch({
          type: 'SELECT_TYPE_DEMANDE',
          payload: {
            publicId: preferred.publicId,
            libelle: preferred.libelle || '',
            delaiMinimumHeures: preferred.delaiMinimumHeures ?? 24,
            joursCelebrationAutorises: preferred.joursCelebrationAutorises || [],
          },
        });
      }
      return;
    }

    if (forfaitsBusy || !forfaits.length) return;

    const preferredForfait = pickPreferredForfait(forfaits, {
      natureHonoraire: draft.prefillNatureHonoraire,
      jourSemaine: draft.horaireJourSemaine,
      currentNature: draft.forfaitNature,
      nombreCelebration: 1,
    });
    if (!preferredForfait) return;

    if (
      draft.forfaitTarifPublicId
      && draft.forfaitTarifPublicId === preferredForfait.publicId
    ) {
      autoAppliedKey.current = key;
      return;
    }

    const n = preferredForfait.nombreCelebration != null
      ? Number(preferredForfait.nombreCelebration)
      : null;
    const multi = isMultiCelebrationForfait(n);
    const natureLabel = NATURE_FORFAIT_OPTIONS.find((o) => o.value === preferredForfait.natureForfait)?.label
      || preferredForfait.nomForfait;
    const dureeLabel = getForfaitDureeLabel(preferredForfait.nombreCelebration);
    dispatch({
      type: draft.forfaitTarifPublicId ? 'SYNC_FORFAIT' : 'SELECT_FORFAIT',
      payload: {
        publicId: preferredForfait.publicId,
        label: multi ? `${natureLabel} · ${dureeLabel}` : natureLabel,
        natureForfait: preferredForfait.natureForfait,
        heurePersonnalise: preferredForfait.heurePersonnalise,
        nombreCelebration: preferredForfait.nombreCelebration,
        nombreJour: preferredForfait.nombreJour ?? preferredForfait.nombreCelebration,
        montantForfait: preferredForfait.montantForfait,
        joursCelebrationAutorises: preferredForfait.joursCelebrationAutorises || [],
      },
    });
    autoAppliedKey.current = key;
  }, [
    draft.prefillFromSchedule,
    draft.paroissePublicId,
    draft.horairePublicId,
    draft.prefillNatureHonoraire,
    draft.horaireJourSemaine,
    draft.typeDemandePublicId,
    draft.forfaitTarifPublicId,
    draft.forfaitNature,
    types,
    forfaits,
    typesBusy,
    forfaitsBusy,
    dispatch,
  ]);

  const summary = [
    draft.typeDemandeLibelle,
    draft.forfaitLabel,
    draft.forfaitMontant != null ? formatCurrency(Number(draft.forfaitMontant)) : null,
  ].filter(Boolean).join(' · ');

  const scheduleHint = lockedFromSchedule
    ? [
      draft.paroisseNom,
      draft.horaireLibelle || draft.horaireHeureCelebration,
      draft.horaireJourSemaine ? WEEK_DAY_LABELS[draft.horaireJourSemaine] || draft.horaireJourSemaine : null,
      formatDateShort(draft.dateDebut),
    ].filter(Boolean).join(' · ')
    : '';

  return (
    <AppCard
      title="Où et pour quelle messe ?"
      subtitle={
        lockedFromSchedule
          ? 'Créneau déjà choisi : paroisse et formule sont figées pour cette messe.'
          : 'Choisissez la paroisse, puis la formule (messe, triduum, neuvaine…).'
      }
    >
      <div className="stack demande-choice-steps" style={{ gap: 18 }}>
        {scheduleHint ? (
          <div className="demande-schedule-prefill" role="status">
            <strong>Créneau sélectionné (figé)</strong>
            <p>{scheduleHint}</p>
            <small className="muted">
              Pour une autre formule (triduum, neuvaine…) ou un autre jour, choisissez un autre créneau
              depuis les horaires — vous ne pouvez pas changer la formule ici.
            </small>
            <div className="button-row" style={{ marginTop: 10 }}>
              <Link to="/horaires" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
                Choisir un autre créneau
              </Link>
            </div>
          </div>
        ) : null}

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
            disabled={loadingParishes || lockedFromSchedule}
            required
            onChange={(id) => {
              if (lockedFromSchedule) return;
              const p = paroisses.find((x) => x.publicId === id);
              dispatch({
                type: 'SELECT_PAROISSE',
                payload: { publicId: id, nom: p?.nom || '' },
              });
            }}
          />
          {lockedFromSchedule ? (
            <small className="muted">Paroisse figée par le créneau choisi.</small>
          ) : null}
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
                      {lockedFromSchedule ? ' · figée pour ce créneau' : ''}
                    </small>
                  ) : null}
                </>
              ) : (
                <p className="muted" style={{ margin: 0 }}>
                  {typesBusy || forfaitsBusy
                    ? 'Préparation de la formule depuis le créneau…'
                    : 'Triduum, neuvaine, trentaine ou messe unique — choisissez dans le panneau.'}
                </p>
              )}
              {!lockedFromSchedule ? (
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
              ) : null}
            </div>
          ) : null}
        </div>
      </div>

      {!lockedFromSchedule ? (
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
      ) : null}
    </AppCard>
  );
}
