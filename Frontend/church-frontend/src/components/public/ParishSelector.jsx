import React from 'react';
import AppCard from '../ui/AppCard';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useParoissesPublicQuery } from '../../hooks/queries/usePublicReferentiel';

export default function ParishSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: paroisses = [], isLoading, error } = useParoissesPublicQuery();

  return (
    <AppCard title="Paroisse" subtitle="Référentiel public /paroisses.">
      {error ? (
        <p className="text-red-600">{error.message}</p>
      ) : null}
      {isLoading ? <p className="muted">Chargement des paroisses…</p> : null}
      <div className="form-field">
        <label htmlFor="public-parish">Paroisse *</label>
        <select
          id="public-parish"
          className="select"
          value={draft.paroissePublicId}
          onChange={(e) => {
            const id = e.target.value;
            const p = paroisses.find((x) => x.publicId === id);
            dispatch({
              type: 'SELECT_PAROISSE',
              payload: { publicId: id, nom: p?.nom || '' },
            });
          }}
        >
          <option value="">— Choisir une paroisse —</option>
          {paroisses.map((p) => (
            <option key={p.publicId} value={p.publicId}>
              {p.nom}
              {p.localiteVille ? ` (${p.localiteVille})` : ''}
            </option>
          ))}
        </select>
      </div>
    </AppCard>
  );
}
