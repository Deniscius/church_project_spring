import React from 'react';
import AppCard from '../ui/AppCard';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useForfaitsActifsQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatCurrency } from '../../utils/formatCurrency';

export default function PricingSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: forfaits = [], isLoading, error, isFetching } = useForfaitsActifsQuery(
    draft.typeDemandePublicId
  );

  const disabled = !draft.typeDemandePublicId;

  return (
    <AppCard title="Forfait / tarif" subtitle="Forfaits actifs pour le type de demande.">
      {disabled ? (
        <p className="muted">Sélectionnez d’abord un type de demande.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading || isFetching ? <p className="muted">Chargement…</p> : null}
      <div className="form-field">
        <label htmlFor="public-forfait">Forfait *</label>
        <select
          id="public-forfait"
          className="select"
          disabled={disabled}
          value={draft.forfaitTarifPublicId}
          onChange={(e) => {
            const id = e.target.value;
            const f = forfaits.find((x) => x.publicId === id);
            const label = f?.libelle || f?.nomForfait || f?.codeForfait || '';
            dispatch({
              type: 'SELECT_FORFAIT',
              payload: {
                publicId: id,
                label,
                heurePersonnalise: f?.heurePersonnalise,
                nombreCelebration: f?.nombreCelebration,
                montantForfait: f?.montantForfait,
              },
            });
          }}
        >
          <option value="">— Choisir un forfait —</option>
          {forfaits.map((f) => (
            <option key={f.publicId} value={f.publicId}>
              {f.libelle || f.nomForfait || f.codeForfait} —{' '}
              {formatCurrency(f.montantForfait != null ? Number(f.montantForfait) : 0)}
            </option>
          ))}
        </select>
      </div>
    </AppCard>
  );
}
