import React, { useMemo, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import {
  formatAllowedDays,
  getEffectiveAllowedDays,
  isDateAllowedForDays,
} from '../../utils/schedulingUtils';

export default function DatesSelector() {
  const { draft, patch } = usePublicDemandeDraft();
  const [dateError, setDateError] = useState('');
  const n = draft.forfaitNombreCelebration;
  const minimumDate = new Date().toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
  const effectiveAllowedDays = useMemo(
    () => getEffectiveAllowedDays(
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      draft.horaireJourSemaine
    ),
    [
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      draft.horaireJourSemaine,
    ]
  );
  const allowedDaysLabel = formatAllowedDays(effectiveAllowedDays);
  const disabled = !draft.forfaitTarifPublicId;

  const handleDateChange = (value) => {
    if (!value) {
      setDateError('');
      patch({ dateDebut: '' });
      return;
    }
    if (value < minimumDate) {
      setDateError('La date doit être ultérieure ou égale à aujourd’hui.');
      patch({ dateDebut: value });
      return;
    }
    if (!isDateAllowedForDays(value, effectiveAllowedDays)) {
      setDateError(`Ce jour n’est pas autorisé pour le forfait choisi (${allowedDaysLabel}).`);
      patch({ dateDebut: value });
      return;
    }
    setDateError('');
    patch({ dateDebut: value });
  };

  return (
    <AppCard
      title="Date de début"
      subtitle={
        disabled
          ? 'Sélectionnez d’abord un forfait.'
          : n != null && n > 1
            ? `Le backend planifie ${n} célébration(s) à partir de cette date, selon les jours du forfait.`
            : 'Date de la première célébration (obligatoire).'
      }
    >
      {disabled ? <p className="muted">Le calendrier s’active après le choix du forfait.</p> : null}
      <div className="form-field">
        <label htmlFor="public-date-debut">Date *</label>
        <AppInput
          id="public-date-debut"
          type="date"
          min={minimumDate}
          value={draft.dateDebut}
          disabled={disabled}
          onChange={(e) => handleDateChange(e.target.value)}
          required
        />
        {dateError ? <small className="text-red-600">{dateError}</small> : null}
        {!disabled ? (
          <small className="muted">
            Jours autorisés pour ce forfait : {allowedDaysLabel}. Délai minimum : {draft.typeDemandeDelaiMinimumHeures} heure(s).
          </small>
        ) : null}
      </div>
    </AppCard>
  );
}
