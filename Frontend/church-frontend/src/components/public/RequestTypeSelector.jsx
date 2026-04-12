import React from 'react';
import AppCard from '../ui/AppCard';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useTypeDemandesByParishQuery } from '../../hooks/queries/usePublicReferentiel';

export default function RequestTypeSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: types = [], isLoading, error, isFetching } = useTypeDemandesByParishQuery(
    draft.paroissePublicId
  );

  const disabled = !draft.paroissePublicId;

  return (
    <AppCard title="Type de demande" subtitle="Types actifs pour la paroisse choisie.">
      {disabled ? (
        <p className="muted">Sélectionnez d’abord une paroisse.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading || isFetching ? <p className="muted">Chargement…</p> : null}
      <div className="form-field">
        <label htmlFor="public-type-demande">Type *</label>
        <select
          id="public-type-demande"
          className="select"
          disabled={disabled}
          value={draft.typeDemandePublicId}
          onChange={(e) => {
            const id = e.target.value;
            const t = types.find((x) => x.publicId === id);
            dispatch({
              type: 'SELECT_TYPE_DEMANDE',
              payload: { publicId: id, libelle: t?.libelle || '' },
            });
          }}
        >
          <option value="">— Choisir un type —</option>
          {types.map((t) => (
            <option key={t.publicId} value={t.publicId}>
              {t.libelle}
            </option>
          ))}
        </select>
      </div>
    </AppCard>
  );
}
