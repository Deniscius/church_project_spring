import React from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';

export default function DatesSelector() {
  const { draft, patch } = usePublicDemandeDraft();
  const n = draft.forfaitNombreCelebration;

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
          value={draft.dateDebut}
          onChange={(e) => patch({ dateDebut: e.target.value })}
          required
        />
      </div>
    </AppCard>
  );
}
