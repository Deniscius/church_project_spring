import React from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useHorairesByParishQuery } from '../../hooks/queries/usePublicReferentiel';

export default function ScheduleSelector() {
  const { draft, dispatch, patch } = usePublicDemandeDraft();
  const { data: horaires = [], isLoading, error, isFetching } = useHorairesByParishQuery(
    draft.paroissePublicId
  );

  const disabled = !draft.paroissePublicId || !draft.forfaitTarifPublicId;
  const hp = draft.forfaitHeurePersonnalise;

  return (
    <AppCard
      title="Horaire"
      subtitle={
        hp
          ? 'Choisissez un créneau de paroisse et/ou une heure personnalisée (au moins l’un des deux).'
          : 'Un horaire de la paroisse est obligatoire pour ce forfait.'
      }
    >
      {disabled ? (
        <p className="muted">Sélectionnez paroisse et forfait.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {isLoading || isFetching ? <p className="muted">Chargement des horaires…</p> : null}
      <div className="form-field">
        <label htmlFor="public-horaire">Horaire de paroisse{hp ? '' : ' *'}</label>
        <select
          id="public-horaire"
          className="select"
          disabled={disabled}
          value={draft.horairePublicId}
          onChange={(e) => {
            const id = e.target.value;
            const h = horaires.find((x) => x.publicId === id);
            dispatch({
              type: 'SELECT_HORAIRE',
              payload: {
                publicId: id,
                libelle: h
                  ? `${h.jourSemaine || ''} ${h.heureCelebration || ''} ${h.libelle || ''}`.trim()
                  : '',
              },
            });
          }}
        >
          <option value="">— Aucun / à préciser —</option>
          {horaires.map((h) => (
            <option key={h.publicId} value={h.publicId}>
              {[h.jourSemaine, h.heureCelebration, h.libelle].filter(Boolean).join(' · ')}
            </option>
          ))}
        </select>
      </div>
      {hp ? (
        <div className="form-field">
          <label htmlFor="public-heure-perso">Heure personnalisée</label>
          <AppInput
            id="public-heure-perso"
            type="time"
            value={draft.heurePersonnalisee}
            onChange={(e) => patch({ heurePersonnalisee: e.target.value })}
          />
        </div>
      ) : null}
    </AppCard>
  );
}
