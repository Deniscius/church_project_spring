import React from 'react';
import AppCard from '../ui/AppCard';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useTypePaiementsPublicQuery } from '../../hooks/queries/usePublicReferentiel';

export default function PaymentTypeSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: types = [], isLoading, error } = useTypePaiementsPublicQuery();

  return (
    <AppCard title="Type de paiement" subtitle="Moyen envisagé (enregistré sur la demande).">
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}
      <div className="form-field">
        <label htmlFor="public-type-paiement">Mode *</label>
        <select
          id="public-type-paiement"
          className="select"
          value={draft.typePaiementPublicId}
          onChange={(e) => {
            const id = e.target.value;
            const t = types.find((x) => x.publicId === id);
            dispatch({
              type: 'SELECT_PAIEMENT',
              payload: {
                publicId: id,
                libelle: t ? `${t.libelle}${t.mode ? ` (${t.mode})` : ''}` : '',
              },
            });
          }}
        >
          <option value="">— Choisir —</option>
          {types.map((t) => (
            <option key={t.publicId} value={t.publicId}>
              {t.libelle}
              {t.mode ? ` — ${t.mode}` : ''}
            </option>
          ))}
        </select>
      </div>
    </AppCard>
  );
}
