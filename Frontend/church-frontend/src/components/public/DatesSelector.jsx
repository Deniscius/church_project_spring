import React from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';

export default function DatesSelector() {
  const { draft, patch } = usePublicDemandeDraft();
  const n = draft.forfaitNombreCelebration;
  const minimumDate = new Date().toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });

  return (
    <AppCard
      title="Date de début"
      subtitle={
        n != null && n > 1
          ? `Le backend planifie ${n} célébration(s) à partir de cette date (jours consécutifs).`
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
          onChange={(e) => patch({ dateDebut: e.target.value })}
          required
        />
      </div>
      <p className="muted" style={{ marginBottom: 0 }}>
        Le système vérifie aussi l’heure choisie et le délai minimum de {draft.typeDemandeDelaiMinimumHeures} heure(s).
      </p>
    </AppCard>
  );
}
