import React, { useMemo, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppSelect from '../ui/AppSelect';
import { getForfaitDureeLabel, isMultiCelebrationForfait, WEEK_DAY_LABELS } from '../../constants/enums';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { useHorairesByParishQuery } from '../../hooks/queries/usePublicReferentiel';
import {
  computeCelebrationDates,
  formatAllowedDays,
  getDayEnumFromDateString,
  getEffectiveAllowedDays,
  getMinimumCelebrationDateIso,
  isDateAllowedForDays,
  listUpcomingAllowedDates,
  resolveHorairesForDate,
} from '../../utils/schedulingUtils';
import { formatTime as formatTimeDisplay } from '../../utils/formatTime';

function formatFrDate(iso) {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}

function emptySchedule() {
  return {
    horairePublicId: '',
    horaireLibelle: '',
    heureCelebration: '',
    jourSemaine: '',
    heurePersonnalisee: '',
  };
}

export default function DatesSelector() {
  const { draft, patch } = usePublicDemandeDraft();
  const [slotError, setSlotError] = useState('');
  const n = Number(draft.forfaitNombreCelebration) || 0;
  const multi = isMultiCelebrationForfait(n);
  const dureeLabel = getForfaitDureeLabel(n);
  const windowDays = draft.forfaitNombreJour > 0 ? Number(draft.forfaitNombreJour) : n;
  const minimumDate = getMinimumCelebrationDateIso(draft.typeDemandeDelaiMinimumHeures ?? 24);
  const hp = Boolean(draft.forfaitHeurePersonnalise);

  const { data: horaires = [], isLoading, error, isFetching } = useHorairesByParishQuery(
    draft.paroissePublicId
  );
  const loadingHoraires = isLoading || isFetching;

  const effectiveAllowedDays = useMemo(
    () => getEffectiveAllowedDays(
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      // Multi : les dates ne sont plus contraintes par un horaire unique.
      multi ? null : draft.horaireJourSemaine
    ),
    [
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      draft.horaireJourSemaine,
      multi,
    ]
  );
  const allowedDaysLabel = formatAllowedDays(effectiveAllowedDays);

  // Date d'abord, puis horaire (ScheduleSelector en simple / créneaux par date en multi).
  const disabled = !draft.forfaitTarifPublicId;

  const startDate = draft.dateDebut || '';
  const dateSchedules = draft.dateSchedules || {};

  const generatedDates = useMemo(() => {
    if (!multi || !startDate) return [];
    return computeCelebrationDates(startDate, effectiveAllowedDays, n);
  }, [multi, startDate, effectiveAllowedDays, n]);

  const lastAllowed = startDate && multi
    ? (() => {
        const d = new Date(`${startDate}T12:00:00`);
        d.setDate(d.getDate() + Math.max(windowDays, 1) - 1);
        return d.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
      })()
    : null;

  const suggestedDates = useMemo(() => {
    if (disabled) return [];
    return listUpcomingAllowedDates(minimumDate, effectiveAllowedDays, {
      count: multi ? Math.max(n + 8, 20) : 16,
    });
  }, [disabled, minimumDate, effectiveAllowedDays, multi, n]);

  const validateStart = (value) => {
    if (!value) return 'La date de début est obligatoire.';
    if (value < minimumDate) {
      return `La date doit respecter le délai minimum (${draft.typeDemandeDelaiMinimumHeures ?? 24} h).`;
    }
    if (!isDateAllowedForDays(value, effectiveAllowedDays)) {
      return `Ce jour n’est pas autorisé pour cette nature de messe (${allowedDaysLabel}).`;
    }
    if (multi) {
      const computed = computeCelebrationDates(value, effectiveAllowedDays, n);
      if (computed.length !== n) {
        return `Impossible de planifier ${n} célébration(s) à partir de cette date avec les jours autorisés.`;
      }
      if (windowDays > 0) {
        const end = new Date(`${value}T12:00:00`);
        end.setDate(end.getDate() + windowDays - 1);
        const endIso = end.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
        const last = computed[computed.length - 1];
        if (last > endIso) {
          return `La série dépasse la période du ${dureeLabel.toLowerCase()} (${windowDays} jour(s)).`;
        }
      }
    }
    return '';
  };

  const commitStart = (value) => {
    const err = value ? validateStart(value) : '';
    setSlotError(err);

    if (!value) {
      patch({ dateDebut: '', datesCelebration: [], dateSchedules: {} });
      return;
    }

    if (multi) {
      const computed = err ? [] : computeCelebrationDates(value, effectiveAllowedDays, n);
      const nextSchedules = {};
      for (const iso of computed) {
        nextSchedules[iso] = dateSchedules[iso] || emptySchedule();
      }
      patch({
        dateDebut: value,
        datesCelebration: computed,
        dateSchedules: nextSchedules,
        // Plus d'horaire unique au niveau demande pour le multi.
        horairePublicId: '',
        horaireLibelle: '',
        horaireHeureCelebration: '',
        horaireJourSemaine: '',
        heurePersonnalisee: '',
      });
      return;
    }

    patch({
      dateDebut: value,
      datesCelebration: [],
    });
  };

  const patchDateSchedule = (iso, updates) => {
    patch({
      dateSchedules: {
        ...dateSchedules,
        [iso]: {
          ...(dateSchedules[iso] || emptySchedule()),
          ...updates,
        },
      },
    });
  };

  const title = multi ? `Date de début du ${dureeLabel.toLowerCase()}` : 'Date de célébration';
  const subtitle = disabled
    ? 'Sélectionnez d’abord la nature de la messe.'
    : multi
      ? `Indiquez la date de début : les ${n} dates (jours autorisés) sont calculées, puis choisissez l’horaire de chaque jour.`
      : 'Choisissez la date, puis l’heure de célébration à l’étape suivante.';

  const dayHint = effectiveAllowedDays?.length === 1
    ? `Uniquement les ${WEEK_DAY_LABELS[effectiveAllowedDays[0]] || effectiveAllowedDays[0]}s.`
    : `Jours autorisés pour cette nature : ${allowedDaysLabel}.`;

  const dateOptions = useMemo(
    () => suggestedDates.map((iso) => ({ value: iso, label: formatFrDate(iso) })),
    [suggestedDates]
  );

  return (
    <AppCard title={title} subtitle={subtitle}>
      {disabled ? (
        <p className="muted">Le calendrier s’active après le choix de la nature de la messe.</p>
      ) : null}
      {!disabled ? (
        <small className="muted" style={{ display: 'block', marginBottom: 12 }}>
          {dayHint} Délai minimum : {draft.typeDemandeDelaiMinimumHeures ?? 24} heure(s).
          {multi ? ` Période du ${dureeLabel.toLowerCase()} : ${windowDays} jour(s) consécutifs.` : ''}
        </small>
      ) : null}

      <div className="form-field">
        <label htmlFor="public-date-start">
          {multi ? 'Date de début *' : 'Date *'}
        </label>
        {dateOptions.length > 0 ? (
          <AppSelect
            id="public-date-start"
            value={startDate}
            disabled={disabled}
            required
            placeholder="— Choisir une date —"
            options={dateOptions}
            onChange={(iso) => commitStart(iso)}
          />
        ) : (
          <AppInput
            id="public-date-start"
            type="date"
            min={minimumDate}
            value={startDate}
            disabled={disabled}
            onChange={(e) => commitStart(e.target.value)}
            required
          />
        )}
        {slotError ? <small className="text-red-600">{slotError}</small> : null}
      </div>

      {multi && generatedDates.length === n ? (
        <div className="generated-dates-preview">
          <p className="muted" style={{ margin: '0 0 0.5rem' }}>
            Calendrier généré ({n} célébrations)
            {lastAllowed ? ` — jusqu’au ${formatFrDate(lastAllowed)}` : ''}
          </p>
          {error ? <p className="text-red-600">{error.message}</p> : null}
          {loadingHoraires ? <p className="muted">Chargement des horaires…</p> : null}
          <ol className="generated-dates-list">
            {generatedDates.map((iso, index) => {
              const day = getDayEnumFromDateString(iso);
              const dayHoraires = resolveHorairesForDate(horaires, iso);
              const schedule = dateSchedules[iso] || emptySchedule();
              const selectedOk = dayHoraires.some((h) => h.publicId === schedule.horairePublicId);
              const horaireOptions = dayHoraires.map((h) => ({
                value: h.publicId,
                label: [formatTimeDisplay(h.heureCelebration), h.libelle].filter(Boolean).join(' · '),
              }));

              return (
                <li key={iso} style={{ display: 'grid', gap: 8, marginBottom: 12 }}>
                  <div>
                    <span>Célébration {index + 1}</span>
                    {' · '}
                    <strong>{formatFrDate(iso)}</strong>
                  </div>
                  <div className="form-field" style={{ margin: 0 }}>
                    <label htmlFor={`slot-horaire-${iso}`}>
                      Horaire du {WEEK_DAY_LABELS[day] || day}{hp ? '' : ' *'}
                    </label>
                    <AppSelect
                      id={`slot-horaire-${iso}`}
                      value={selectedOk ? schedule.horairePublicId : ''}
                      placeholder="— Choisir un créneau —"
                      options={horaireOptions}
                      onChange={(id) => {
                        const h = dayHoraires.find((x) => x.publicId === id);
                        patchDateSchedule(iso, {
                          horairePublicId: id,
                          horaireLibelle: h
                            ? `${h.heureCelebration || ''} ${h.libelle || ''}`.trim()
                            : '',
                          heureCelebration: h?.heureCelebration || '',
                          jourSemaine: h?.jourSemaine || day,
                        });
                      }}
                    />
                    {dayHoraires.length === 0 && !loadingHoraires ? (
                      <small className={hp ? 'muted' : 'text-red-600'}>
                        Aucun horaire paroissial ce jour-là.
                        {hp ? ' Indiquez une heure personnalisée.' : ' Contactez la paroisse.'}
                      </small>
                    ) : (
                      <small className="muted">
                        Créneaux au programme pour le {WEEK_DAY_LABELS[day] || day}.
                      </small>
                    )}
                  </div>
                  {hp ? (
                    <div className="form-field" style={{ margin: 0 }}>
                      <label htmlFor={`slot-perso-${iso}`}>Heure personnalisée</label>
                      <AppInput
                        id={`slot-perso-${iso}`}
                        type="time"
                        value={schedule.heurePersonnalisee || ''}
                        onChange={(e) => patchDateSchedule(iso, { heurePersonnalisee: e.target.value })}
                      />
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ol>
        </div>
      ) : null}
    </AppCard>
  );
}
