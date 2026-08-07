import React, { useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import { FieldLabel } from '../ui/HelpTip';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useParoissesPublicQuery } from '../../hooks/queries/usePublicReferentiel';
import { HELP } from '../../constants/helpTips';

export default function ParishSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: paroisses = [], isLoading, error } = useParoissesPublicQuery();

  const options = useMemo(
    () => paroisses.map((p) => ({
      value: p.publicId,
      label: p.doyenneNom ? `${p.nom} (${p.doyenneNom})` : p.nom,
    })),
    [paroisses]
  );

  return (
    <AppCard
      title="Paroisse"
      subtitle="Saisissez le nom de votre paroisse — les suggestions s’affichent au fur et à mesure."
    >
      {error ? (
        <p className="text-red-600">{error.message}</p>
      ) : null}
      {isLoading ? <p className="muted">Chargement des paroisses…</p> : null}
      <div className="form-field">
        <FieldLabel htmlFor="public-parish" help={HELP.demande.paroisse} required>
          Paroisse
        </FieldLabel>
        <AppSelect
          id="public-parish"
          name="paroissePublicId"
          required
          searchable
          searchPlaceholder="Tapez le nom de la paroisse…"
          placeholder="— Saisir ou choisir une paroisse —"
          value={draft.paroissePublicId}
          options={options}
          disabled={isLoading}
          onChange={(id) => {
            const p = paroisses.find((x) => x.publicId === id);
            dispatch({
              type: 'SELECT_PAROISSE',
              payload: { publicId: id, nom: p?.nom || '' },
            });
          }}
        />
      </div>
    </AppCard>
  );
}
