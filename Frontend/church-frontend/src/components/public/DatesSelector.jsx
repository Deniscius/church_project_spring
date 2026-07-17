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
      draft.horaireJourSemaine
    ),
    [draft.typeDemandeJoursCelebrationAutorises, draft.horaireJourSemaine]
  );
  const allowedDaysLabel = formatAllowedDays(effectiveAllowedDays);

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
      setDateError(`Ce jour n’est pas autorisé pour ce type de demande (${allowedDaysLabel}).`);
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
        n != null && n > 1
          ? `Le backend planifie ${n} célébration(s) à partir de cette date, uniquement les jours autorisés.`
          : 'Date de la première célébration (obligatoire).'
      }
    >
      <div className="form-field">
        <label htmlFor="public-date-debut">Date *</label>
        <AppInput
          id="public-date-debut"
          type="date"
          min={minimumDate}
          value={draft.dateDebut}
          onChange={(e) => handleDateChange(e.target.value)}
          required
        />
        {dateError ? <small className="text-red-600">{dateError}</small> : null}
        <small className="muted">
          Jours autorisés : {allowedDaysLabel}. Délai minimum : {draft.typeDemandeDelaiMinimumHeures} heure(s).
        </small>
      </div>
    </AppCard>
  );
}
