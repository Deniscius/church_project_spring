import React, { useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppSelect from '../ui/AppSelect';
import { FieldLabel } from '../ui/HelpTip';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useTypeDemandesByParishQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatAllowedDays } from '../../utils/schedulingUtils';
import { HELP } from '../../constants/helpTips';
import { PRIMARY_REQUEST_TYPE_LABELS } from '../../constants/enums';

export default function RequestTypeSelector() {
  const { draft, dispatch } = usePublicDemandeDraft();
  const { data: types = [], isLoading, error, isFetching } = useTypeDemandesByParishQuery(
    draft.paroissePublicId
  );

  const disabled = !draft.paroissePublicId;
  const loading = isLoading || isFetching;
  const noType = !disabled && !loading && !error && types.length === 0;

  const options = useMemo(
    () => types.map((t) => {
      const category = PRIMARY_REQUEST_TYPE_LABELS[t.typeDemandeEnum] || t.typeDemandeEnum;
      return {
        value: t.publicId,
        label: category ? `${t.libelle} (${category})` : t.libelle,
      };
    }),
    [types]
  );

  return (
    <AppCard title="Type de demande" subtitle="Types actifs pour la paroisse choisie.">
      {disabled ? (
        <p className="muted">Sélectionnez d’abord une paroisse.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      {noType ? (
        <p className="text-red-600">
          Aucun type de demande n’est configuré pour cette paroisse. Contactez la paroisse.
        </p>
      ) : null}
      <div className="form-field">
        <FieldLabel htmlFor="public-type-demande" help={HELP.demande.typeDemande} required>
          Type
        </FieldLabel>
        <AppSelect
          id="public-type-demande"
          name="typeDemandePublicId"
          required
          disabled={disabled || loading}
          placeholder="— Choisir un type —"
          value={draft.typeDemandePublicId}
          options={options}
          onChange={(id) => {
            const t = types.find((x) => x.publicId === id);
            const jours = t?.joursCelebrationAutorises || [];
            const delai = t?.delaiMinimumHeures ?? 24;
            dispatch({
              type: 'SELECT_TYPE_DEMANDE',
              payload: {
                publicId: id,
                libelle: t?.libelle || '',
                delaiMinimumHeures: delai,
                joursCelebrationAutorises: jours,
              },
            });
          }}
        />
        {draft.typeDemandePublicId ? (
          <small className="muted">
            À déposer au moins {draft.typeDemandeDelaiMinimumHeures} heure(s) avant la célébration.
            {draft.typeDemandeJoursCelebrationAutorises?.length ? (
              <> Jours autorisés : {formatAllowedDays(draft.typeDemandeJoursCelebrationAutorises)}.</>
            ) : null}
          </small>
        ) : null}
      </div>
    </AppCard>
  );
}
