import React, { useEffect, useMemo } from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppSelect from '../ui/AppSelect';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useHorairesByParishQuery } from '../../hooks/queries/usePublicReferentiel';
import {
  formatAllowedDays,
  getDayEnumFromDateString,
  resolveAllowedDays,
  resolveHorairesForDate,
} from '../../utils/schedulingUtils';
import { formatParishTimeInUserZone } from '../../utils/formatTime';

/** Sélecteur d'horaire pour une célébration unique (le multi gère ses créneaux dans DatesSelector). */
export default function ScheduleSelector() {
  const { draft, dispatch, patch } = usePublicDemandeDraft();
  const { data: horaires = [], isLoading, error, isFetching } = useHorairesByParishQuery(
    draft.paroissePublicId
  );

  const lockedFromSchedule = Boolean(
    draft.prefillFromSchedule && (draft.horairePublicId || draft.horaireJourSemaine)
  );
  const disabled = !draft.paroissePublicId || !draft.forfaitTarifPublicId || !draft.dateDebut;
  const hp = draft.forfaitHeurePersonnalise;
  const loading = isLoading || isFetching;

  const allowedDays = useMemo(
    () => resolveAllowedDays(
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises
    ),
    [
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
    ]
  );

  const filteredHoraires = useMemo(() => {
    if (!draft.dateDebut) return [];

    const byProgramme = resolveHorairesForDate(horaires, draft.dateDebut);
    const messeUnique = byProgramme.some((h) => h.uniqueSurParoisse);
    if (messeUnique) return byProgramme;

    if (!allowedDays?.length) return byProgramme;
    return byProgramme.filter(
      (h) => !h.jourSemaine || allowedDays.includes(h.jourSemaine)
    );
  }, [horaires, allowedDays, draft.dateDebut]);

  const messeUnique = Boolean(
    draft.dateDebut && filteredHoraires.length > 0 && filteredHoraires.every((h) => h.uniqueSurParoisse)
  );
  const uniqueSlot = messeUnique ? filteredHoraires[0] : null;

  const horaireOptions = useMemo(() => {
    const opts = filteredHoraires.map((h) => ({
      value: h.publicId,
      label: [formatParishTimeInUserZone(h.heureCelebration), h.libelle].filter(Boolean).join(' · '),
    }));
    if (
      draft.horairePublicId
      && !opts.some((o) => o.value === draft.horairePublicId)
      && draft.horaireLibelle
    ) {
      opts.unshift({
        value: draft.horairePublicId,
        label: draft.horaireLibelle,
      });
    }
    return opts;
  }, [filteredHoraires, draft.horairePublicId, draft.horaireLibelle]);

  useEffect(() => {
    if (!uniqueSlot?.publicId) return;
    if (draft.horairePublicId === uniqueSlot.publicId && !draft.heurePersonnalisee) return;
    dispatch({
      type: 'SELECT_HORAIRE',
      payload: {
        publicId: uniqueSlot.publicId,
        libelle: `${uniqueSlot.jourSemaine || ''} ${uniqueSlot.heureCelebration || ''} ${uniqueSlot.libelle || ''}`.trim(),
        heureCelebration: uniqueSlot.heureCelebration || '',
        jourSemaine: uniqueSlot.jourSemaine || '',
        preserveDate: true,
      },
    });
    if (draft.heurePersonnalisee) {
      patch({ heurePersonnalisee: '' });
    }
  }, [
    uniqueSlot?.publicId,
    uniqueSlot?.heureCelebration,
    uniqueSlot?.jourSemaine,
    uniqueSlot?.libelle,
    draft.horairePublicId,
    draft.heurePersonnalisee,
    dispatch,
    patch,
  ]);

  useEffect(() => {
    if (!draft.dateDebut || !draft.horairePublicId || messeUnique) return;
    const stillValid = filteredHoraires.some((h) => h.publicId === draft.horairePublicId);
    if (stillValid) return;
    if (draft.prefillFromSchedule && draft.horaireJourSemaine) {
      const day = getDayEnumFromDateString(draft.dateDebut);
      if (day === draft.horaireJourSemaine) return;
    }
    dispatch({
      type: 'SELECT_HORAIRE',
      payload: {
        publicId: '',
        libelle: '',
        heureCelebration: '',
        jourSemaine: '',
        preserveDate: true,
      },
    });
  }, [
    draft.dateDebut,
    draft.horairePublicId,
    draft.prefillFromSchedule,
    draft.horaireJourSemaine,
    filteredHoraires,
    messeUnique,
    dispatch,
  ]);

  const noHoraire = !disabled && !loading && !error && filteredHoraires.length === 0;
  const filteredOutAll = !disabled && !loading && horaires.length > 0 && filteredHoraires.length === 0;
  const allowHeurePerso = hp && !messeUnique;

  const uniqueHeureLabel = uniqueSlot
    ? [formatParishTimeInUserZone(uniqueSlot.heureCelebration), uniqueSlot.libelle].filter(Boolean).join(' · ')
    : '';

  const lockedHeureLabel = [
    formatParishTimeInUserZone(draft.horaireHeureCelebration),
    draft.horaireLibelle,
  ].filter(Boolean).join(' · ')
    || draft.horaireLibelle
    || '—';

  return (
    <AppCard
      title="Heure de célébration"
      subtitle={
        lockedFromSchedule
          ? 'Heure figée par le créneau choisi.'
          : !draft.dateDebut
            ? 'Choisissez d’abord la date de célébration.'
            : messeUnique
              ? 'Messe unique ce jour-là : l’heure est imposée. Changez de date ci-dessus pour un autre créneau.'
              : allowHeurePerso
                ? 'Choisissez un créneau de paroisse et/ou une heure personnalisée (au moins l’un des deux).'
                : 'Un horaire de la paroisse est obligatoire pour ce forfait.'
      }
    >
      {disabled && !draft.dateDebut ? (
        <p className="muted">Sélectionnez d’abord une date ci-dessus.</p>
      ) : null}
      {error ? <p className="text-red-600">{error.message}</p> : null}
      {loading ? <p className="muted">Chargement des horaires…</p> : null}

      {lockedFromSchedule && draft.dateDebut ? (
        <div className="form-field">
          <label htmlFor="public-horaire-locked">Horaire (figé)</label>
          <AppInput id="public-horaire-locked" value={lockedHeureLabel} readOnly disabled />
          <small className="muted">
            Pour une autre heure, choisissez un autre créneau depuis les horaires.
          </small>
        </div>
      ) : null}

      {!lockedFromSchedule && noHoraire ? (
        <p className={allowHeurePerso ? 'muted' : 'text-red-600'}>
          {filteredOutAll
            ? `Aucun horaire paroissial ne correspond aux jours autorisés (${formatAllowedDays(allowedDays)}).`
            : 'Aucune heure de célébration n’est encore configurée pour cette paroisse ce jour-là.'}
          {allowHeurePerso
            ? ' Indiquez ci-dessous l’heure souhaitée.'
            : ' Ce forfait exige un horaire paroissial : contactez la paroisse.'}
        </p>
      ) : null}

      {!lockedFromSchedule && messeUnique && uniqueSlot ? (
        <div className="form-field">
          <label htmlFor="public-horaire-unique">Heure de la messe unique</label>
          <AppInput id="public-horaire-unique" value={uniqueHeureLabel} readOnly disabled />
          <small className="muted">
            Aucune autre heure n’est proposée ce jour-là. Pour un autre créneau, choisissez une autre date.
          </small>
        </div>
      ) : null}

      {!lockedFromSchedule && !noHoraire && !messeUnique && draft.dateDebut ? (
        <div className="form-field">
          <label htmlFor="public-horaire">Horaire de paroisse{allowHeurePerso ? '' : ' *'}</label>
          <AppSelect
            id="public-horaire"
            name="horairePublicId"
            required={!allowHeurePerso}
            disabled={disabled}
            placeholder="— Aucun / à préciser —"
            value={
              horaireOptions.some((o) => o.value === draft.horairePublicId)
                ? draft.horairePublicId
                : ''
            }
            options={horaireOptions}
            onChange={(id) => {
              const h = filteredHoraires.find((x) => x.publicId === id);
              dispatch({
                type: 'SELECT_HORAIRE',
                payload: {
                  publicId: id,
                  libelle: h
                    ? `${h.jourSemaine || ''} ${h.heureCelebration || ''} ${h.libelle || ''}`.trim()
                    : draft.horaireLibelle || '',
                  heureCelebration: h?.heureCelebration || draft.horaireHeureCelebration || '',
                  jourSemaine: h?.jourSemaine || draft.horaireJourSemaine || '',
                  preserveDate: true,
                },
              });
            }}
          />
          <small className="muted">
            Créneaux du programme pour cette date
            {allowedDays?.length ? ` (${formatAllowedDays(allowedDays)})` : ''}.
          </small>
        </div>
      ) : null}

      {!lockedFromSchedule && allowHeurePerso && draft.dateDebut ? (
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
