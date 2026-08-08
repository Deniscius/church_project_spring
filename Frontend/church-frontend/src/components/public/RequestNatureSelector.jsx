import React, { useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import AppAlert from '../ui/AppAlert';
import AppInput from '../ui/AppInput';
import { FieldLabel } from '../ui/HelpTip';
import { getForfaitDureeLabel, isMultiCelebrationForfait, NATURE_FORFAIT_OPTIONS } from '../../constants/enums';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import {
  useForfaitsActifsQuery,
  useHorairesByParishQuery,
} from '../../hooks/queries/usePublicReferentiel';
import {
  formatAllowedDays,
  isMesseUniqueDay,
  resolveReferencedCelebrationDate,
} from '../../utils/schedulingUtils';
import { formatCurrency } from '../../utils/formatCurrency';
import { HELP } from '../../constants/helpTips';

export default function RequestNatureSelector() {
  const { draft, dispatch, patch } = usePublicDemandeDraft();
  const { data: forfaits = [], isLoading, error, isFetching } = useForfaitsActifsQuery(
    draft.typeDemandePublicId
  );
  const { data: horaires = [] } = useHorairesByParishQuery(draft.paroissePublicId);

  const disabled = !draft.typeDemandePublicId;

  const referencedDate = useMemo(() => {
    if (!draft.typeDemandePublicId) return '';
    return resolveReferencedCelebrationDate({
      allowedDays: draft.typeDemandeJoursCelebrationAutorises || [],
      delaiMinimumHeures: draft.typeDemandeDelaiMinimumHeures,
      horaires,
    });
  }, [
    draft.typeDemandePublicId,
    draft.typeDemandeJoursCelebrationAutorises,
    draft.typeDemandeDelaiMinimumHeures,
    horaires,
  ]);

  const forfaitsByNature = useMemo(
    () => Object.fromEntries(
      forfaits.map((forfait) => [forfait.natureForfait, forfait])
    ),
    [forfaits]
  );

  const options = useMemo(
    () => NATURE_FORFAIT_OPTIONS
      .filter((option) => forfaitsByNature[option.value])
      .map((option) => {
        const forfait = forfaitsByNature[option.value];
        const dureeLabel = getForfaitDureeLabel(forfait.nombreCelebration);
        const amount = formatCurrency(
          forfait.montantForfait != null ? Number(forfait.montantForfait) : 0
        );
        const days = forfait.joursCelebrationAutorises?.length
          ? ` · ${formatAllowedDays(forfait.joursCelebrationAutorises)}`
          : '';
        const duree = forfait.nombreCelebration > 1
          ? ` · ${dureeLabel} (${forfait.nombreCelebration} dates)`
          : ' · Célébration unique';
        return {
          value: option.value,
          label: `${option.label} — ${amount}${duree}${days}`,
        };
      }),
    [forfaitsByNature]
  );

  const selectNature = (nature) => {
    const forfait = forfaitsByNature[nature];
    if (!forfait) return;
    const n = forfait.nombreCelebration != null ? Number(forfait.nombreCelebration) : null;
    const multi = isMultiCelebrationForfait(n);

    const natureLabel = NATURE_FORFAIT_OPTIONS.find((option) => option.value === nature)?.label || forfait.nomForfait;
    const dureeLabel = getForfaitDureeLabel(forfait.nombreCelebration);
    const label = multi ? `${natureLabel} · ${dureeLabel}` : natureLabel;
    const forfaitDays = forfait.joursCelebrationAutorises || [];

    dispatch({
      type: 'SELECT_FORFAIT',
      payload: {
        publicId: forfait.publicId,
        label,
        natureForfait: nature,
        heurePersonnalise: forfait.heurePersonnalise,
        nombreCelebration: forfait.nombreCelebration,
        nombreJour: forfait.nombreJour ?? forfait.nombreCelebration,
        montantForfait: forfait.montantForfait,
        joursCelebrationAutorises: forfaitDays,
      },
    });
  };

  const programmeHint = referencedDate
    ? isMesseUniqueDay(horaires, referencedDate)
      ? `Messe unique au programme le ${new Date(`${referencedDate}T12:00:00`).toLocaleDateString('fr-FR', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
      })} — si vous choisissez ce jour, l’heure sera imposée.`
      : `Événement au programme le ${new Date(`${referencedDate}T12:00:00`).toLocaleDateString('fr-FR', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
      })} — vous pourrez le choisir à l’étape date.`
    : null;

  return (
    <AppCard
      title="Nature de la messe"
      subtitle="Ouvrez la liste pour voir les natures et montants disponibles."
    >
      {disabled ? (
        <p className="muted">Sélectionnez d’abord un type de demande.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading || isFetching ? <p className="muted">Chargement…</p> : null}
      {!disabled && !isLoading && !options.length ? (
        <p className="text-red-600">
          Aucun forfait actif n’est configuré pour ce type de demande. Contactez la paroisse.
        </p>
      ) : null}
      {programmeHint ? (
        <p className="muted" style={{ marginTop: 0 }}>{programmeHint}</p>
      ) : null}
      {draft.forfaitNature === 'SPECIALE' ? (
        <AppAlert variant="info">
          Messe spéciale : un numéro de téléphone et une adresse e-mail valides sont obligatoires.
        </AppAlert>
      ) : null}
      <div className="form-field">
        <label htmlFor="public-nature">Nature *</label>
        <AppSelect
          id="public-nature"
          name="forfaitNature"
          required
          disabled={disabled || isLoading || isFetching}
          placeholder="— Choisir une nature —"
          value={draft.forfaitNature || ''}
          options={options}
          onChange={(nature) => selectNature(nature)}
        />
      </div>
      {draft.forfaitNature === 'SPECIALE' ? (
        <div className="form-field">
          <FieldLabel htmlFor="nature-email" help={HELP.demande.email} required>
            E-mail (obligatoire pour une spéciale)
          </FieldLabel>
          <AppInput
            id="nature-email"
            type="email"
            autoComplete="email"
            value={draft.emailFidele || ''}
            onChange={(e) => patch({ emailFidele: e.target.value })}
            placeholder="ex. vous@email.com"
            required
          />
        </div>
      ) : null}
    </AppCard>
  );
}
