import React, { useEffect, useMemo, useState } from 'react';
import AppCard from '../ui/AppCard';
import AppInput from '../ui/AppInput';
import AppSelect from '../ui/AppSelect';
import AppButton from '../ui/AppButton';
import MultiScheduleModal from './MultiScheduleModal';
import {
  getForfaitDureeLabel,
  isMultiCelebrationForfait,
  NATURE_FORFAIT_OPTIONS,
  WEEK_DAY_LABELS,
  WEEK_DAYS,
} from '../../constants/enums';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import {
  useForfaitsActifsQuery,
  useHorairesByParishQuery,
} from '../../hooks/queries/usePublicReferentiel';
import {
  computeCelebrationDates,
  emptySchedule,
  formatAllowedDays,
  getDayEnumFromDateString,
  getEffectiveAllowedDays,
  getMinimumCelebrationDateIso,
  isDateAllowedForDays,
  isTrentaineForfait,
  listUpcomingAllowedDates,
  resolveHorairesForDate,
} from '../../utils/schedulingUtils';
import { formatParishTimeInUserZone, formatTime } from '../../utils/formatTime';
import { pickPreferredForfait } from '../../utils/demandePrefill';
import { formatCurrency } from '../../utils/formatCurrency';

function formatFrDate(iso) {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}

function pickDefaultSchedule(horaires, iso) {
  const day = getDayEnumFromDateString(iso);
  const dayHoraires = resolveHorairesForDate(horaires, iso);
  const first = dayHoraires[0];
  if (!first) return emptySchedule();
  return {
    horairePublicId: first.publicId || '',
    horaireLibelle: [formatTime(first.heureCelebration), first.libelle].filter(Boolean).join(' · '),
    heureCelebration: formatTime(first.heureCelebration) || first.heureCelebration || '',
    jourSemaine: first.jourSemaine || day || '',
    heurePersonnalisee: '',
  };
}

export default function DatesSelector() {
  const { draft, patch, dispatch } = usePublicDemandeDraft();
  const [slotError, setSlotError] = useState('');
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const n = Number(draft.forfaitNombreCelebration) || 0;
  const multi = isMultiCelebrationForfait(n);
  const trentaine = isTrentaineForfait(n);
  const dureeLabel = getForfaitDureeLabel(n);
  const windowDays = trentaine ? 30 : (draft.forfaitNombreJour > 0 ? Number(draft.forfaitNombreJour) : n);
  const minimumDate = getMinimumCelebrationDateIso(draft.typeDemandeDelaiMinimumHeures ?? 24);
  const hp = Boolean(draft.forfaitHeurePersonnalise);

  const { data: horaires = [], isLoading, error, isFetching } = useHorairesByParishQuery(
    draft.paroissePublicId
  );
  const { data: forfaits = [] } = useForfaitsActifsQuery(draft.typeDemandePublicId);
  const loadingHoraires = isLoading || isFetching;

  const effectiveAllowedDays = useMemo(() => {
    if (trentaine) return WEEK_DAYS;
    // Messe unique : union des jours des forfaits actifs → le tarif suit le jour choisi.
    if (!multi && forfaits.length) {
      const singles = forfaits.filter((f) => {
        const count = f.nombreCelebration != null ? Number(f.nombreCelebration) : 1;
        return !Number.isFinite(count) || count <= 1;
      });
      const union = new Set();
      for (const f of (singles.length ? singles : forfaits)) {
        for (const day of (f.joursCelebrationAutorises || [])) union.add(day);
      }
      if (union.size) {
        const typeDays = draft.typeDemandeJoursCelebrationAutorises || [];
        const merged = typeDays.length
          ? [...union].filter((day) => typeDays.includes(day))
          : [...union];
        if (merged.length) {
          // Préremplissage créneau : garder le jour du créneau tant qu’il est valide.
          if (
            draft.prefillFromSchedule
            && draft.horaireJourSemaine
            && merged.includes(draft.horaireJourSemaine)
          ) {
            return [draft.horaireJourSemaine];
          }
          return WEEK_DAYS.filter((day) => merged.includes(day));
        }
      }
    }
    return getEffectiveAllowedDays(
      draft.typeDemandeJoursCelebrationAutorises,
      draft.forfaitJoursCelebrationAutorises,
      multi ? null : draft.horaireJourSemaine
    );
  }, [
    trentaine,
    multi,
    forfaits,
    draft.typeDemandeJoursCelebrationAutorises,
    draft.forfaitJoursCelebrationAutorises,
    draft.horaireJourSemaine,
    draft.prefillFromSchedule,
  ]);
  const allowedDaysLabel = formatAllowedDays(effectiveAllowedDays);
  const lockedFromSchedule = Boolean(
    !multi && draft.prefillFromSchedule && (draft.horairePublicId || draft.horaireJourSemaine)
  );
  const disabled = !draft.forfaitTarifPublicId;
  const startDate = draft.dateDebut || '';
  const dateSchedules = useMemo(
    () => draft.dateSchedules || {},
    [draft.dateSchedules]
  );

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

  const dateOptions = useMemo(() => {
    const opts = suggestedDates.map((iso) => ({ value: iso, label: formatFrDate(iso) }));
    if (startDate && !opts.some((o) => o.value === startDate)) {
      opts.unshift({ value: startDate, label: formatFrDate(startDate) });
    }
    return opts;
  }, [suggestedDates, startDate]);

  const filledCount = useMemo(() => {
    if (!generatedDates.length) return 0;
    return generatedDates.filter((iso) => {
      const s = dateSchedules[iso];
      return s && (s.horairePublicId || (hp && s.heurePersonnalisee));
    }).length;
  }, [generatedDates, dateSchedules, hp]);

  const sundayCount = useMemo(
    () => generatedDates.filter((iso) => getDayEnumFromDateString(iso) === 'DIMANCHE').length,
    [generatedDates]
  );

  // Messe unique : aligne NORMALE / DOMINICALE / SPECIALE sur le jour (et le créneau).
  useEffect(() => {
    if (multi || !draft.dateDebut || !forfaits.length || !draft.forfaitTarifPublicId) return;
    const day = getDayEnumFromDateString(draft.dateDebut);
    if (!day) return;
    const slot = resolveHorairesForDate(horaires, draft.dateDebut)
      .find((h) => h.publicId === draft.horairePublicId);
    const natureHonoraire = slot?.natureHonoraire || draft.prefillNatureHonoraire || '';
    const preferred = pickPreferredForfait(forfaits, {
      jourSemaine: day,
      natureHonoraire,
      currentNature: draft.forfaitNature,
      nombreCelebration: draft.forfaitNombreCelebration ?? 1,
    });
    if (!preferred || preferred.publicId === draft.forfaitTarifPublicId) return;
    const natureLabel = NATURE_FORFAIT_OPTIONS.find((o) => o.value === preferred.natureForfait)?.label
      || preferred.nomForfait;
    dispatch({
      type: 'SYNC_FORFAIT',
      payload: {
        publicId: preferred.publicId,
        label: natureLabel,
        natureForfait: preferred.natureForfait,
        heurePersonnalise: preferred.heurePersonnalise,
        nombreCelebration: preferred.nombreCelebration,
        nombreJour: preferred.nombreJour ?? preferred.nombreCelebration,
        montantForfait: preferred.montantForfait,
        joursCelebrationAutorises: preferred.joursCelebrationAutorises || [],
      },
    });
  }, [
    multi,
    draft.dateDebut,
    draft.horairePublicId,
    draft.forfaitTarifPublicId,
    draft.forfaitNature,
    draft.forfaitNombreCelebration,
    draft.prefillNatureHonoraire,
    forfaits,
    horaires,
    dispatch,
  ]);

  // Trentaine : auto-remplir les horaires dès que les dates + programme sont prêts.
  useEffect(() => {
    if (!trentaine || !generatedDates.length || loadingHoraires || !horaires.length) return;
    let changed = false;
    const next = { ...dateSchedules };
    for (const iso of generatedDates) {
      const current = next[iso];
      if (current?.horairePublicId || current?.heurePersonnalisee) continue;
      next[iso] = pickDefaultSchedule(horaires, iso);
      changed = true;
    }
    if (changed) {
      patch({ dateSchedules: next });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trentaine, generatedDates.join('|'), loadingHoraires, horaires]);

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
        nextSchedules[iso] = trentaine && horaires.length
          ? pickDefaultSchedule(horaires, iso)
          : (dateSchedules[iso] || emptySchedule());
      }
      patch({
        dateDebut: value,
        datesCelebration: computed,
        dateSchedules: nextSchedules,
        horairePublicId: '',
        horaireLibelle: '',
        horaireHeureCelebration: '',
        horaireJourSemaine: '',
        heurePersonnalisee: '',
      });
      if (!err && computed.length === n && !trentaine) {
        setScheduleModalOpen(true);
      }
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

  const applyDefaultToAll = () => {
    const next = { ...dateSchedules };
    for (const iso of generatedDates) {
      next[iso] = pickDefaultSchedule(horaires, iso);
    }
    patch({ dateSchedules: next });
  };

  const title = multi ? `Date de début du ${dureeLabel.toLowerCase()}` : 'Date de célébration';
  const subtitle = disabled
    ? 'Sélectionnez d’abord la nature de la messe.'
    : lockedFromSchedule
      ? 'Date figée par le créneau choisi (ex. messe du dimanche).'
      : multi
        ? trentaine
          ? 'Choisissez le premier jour : les 30 jours suivants sont calculés, les horaires sont préremplis. Ajustez les dimanches si besoin.'
          : `Choisissez la date de début, puis les horaires des ${n} célébrations dans le panneau.`
        : 'Choisissez la date, puis l’heure de célébration ci-dessous.';

  const dayHint = trentaine
    ? 'Trentaine : 30 jours calendaires successifs à partir de la date de début.'
    : effectiveAllowedDays?.length === 1
      ? `Uniquement les ${WEEK_DAY_LABELS[effectiveAllowedDays[0]] || effectiveAllowedDays[0]}s.`
      : `Jours autorisés pour cette nature : ${allowedDaysLabel}.`;

  const tariffHint = !multi && draft.forfaitMontant != null
    ? `Tarif appliqué : ${draft.forfaitLabel || draft.forfaitNature || '—'} · ${formatCurrency(Number(draft.forfaitMontant))} (selon le jour de célébration).`
    : '';

  return (
    <AppCard title={title} subtitle={subtitle}>
      {disabled ? (
        <p className="muted">Le calendrier s’active après le choix de la nature de la messe.</p>
      ) : null}
      {!disabled ? (
        <small className="muted" style={{ display: 'block', marginBottom: 12 }}>
          {dayHint} Délai minimum : {draft.typeDemandeDelaiMinimumHeures ?? 24} heure(s).
          {multi ? ` Période du ${dureeLabel.toLowerCase()} : ${windowDays} jour(s) consécutifs.` : ''}
          {tariffHint ? ` ${tariffHint}` : ''}
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
            disabled={disabled || lockedFromSchedule}
            required
            placeholder="— Choisir une date —"
            options={dateOptions}
            onChange={(iso) => {
              if (lockedFromSchedule) return;
              commitStart(iso);
            }}
          />
        ) : (
          <AppInput
            id="public-date-start"
            type="date"
            min={minimumDate}
            value={startDate}
            disabled={disabled || lockedFromSchedule}
            onChange={(e) => {
              if (lockedFromSchedule) return;
              commitStart(e.target.value);
            }}
            required
          />
        )}
        {lockedFromSchedule ? (
          <small className="muted">Date figée — changez de créneau depuis les horaires pour une autre date.</small>
        ) : null}
        {slotError ? <small className="text-red-600">{slotError}</small> : null}
      </div>

      {multi && generatedDates.length === n ? (
        <div className="generated-dates-preview">
          <p className="muted" style={{ margin: '0 0 0.75rem' }}>
            Calendrier généré ({n} célébrations)
            {lastAllowed ? ` — jusqu’au ${formatFrDate(lastAllowed)}` : ''}
            {trentaine && sundayCount ? ` · ${sundayCount} dimanche(s)` : ''}
          </p>
          {error ? <p className="text-red-600">{error.message}</p> : null}
          {loadingHoraires ? <p className="muted">Chargement des horaires…</p> : null}

          <div className="formule-summary-box" style={{ marginBottom: 12 }}>
            <p className="formule-summary-value" style={{ fontSize: '0.98rem' }}>
              Horaires renseignés : {filledCount}/{n}
            </p>
            <AppButton
              type="button"
              variant="primary"
              disabled={loadingHoraires}
              onClick={() => setScheduleModalOpen(true)}
            >
              {trentaine
                ? 'Ajuster les dimanches'
                : filledCount === n
                  ? 'Modifier les horaires'
                  : 'Choisir les horaires'}
            </AppButton>
          </div>

          <ol className="generated-dates-list generated-dates-list--compact">
            {generatedDates.slice(0, trentaine ? 5 : n).map((iso, index) => {
              const schedule = dateSchedules[iso] || emptySchedule();
              const day = getDayEnumFromDateString(iso);
              return (
                <li key={iso}>
                  <span>J{index + 1}</span>
                  {' · '}
                  <strong>{formatFrDate(iso)}</strong>
                  {day === 'DIMANCHE' ? ' · Dimanche' : ''}
                  {' · '}
                  <span className="muted">
                    {schedule.heurePersonnalisee
                      || formatParishTimeInUserZone(schedule.heureCelebration)
                      || schedule.horaireLibelle
                      || 'horaire à définir'}
                  </span>
                </li>
              );
            })}
            {trentaine && generatedDates.length > 5 ? (
              <li className="muted">… et {generatedDates.length - 5} autres jours</li>
            ) : null}
          </ol>
        </div>
      ) : null}

      <MultiScheduleModal
        open={scheduleModalOpen}
        onClose={() => setScheduleModalOpen(false)}
        dates={generatedDates}
        horaires={horaires}
        dateSchedules={dateSchedules}
        onChangeSchedule={patchDateSchedule}
        onApplyDefaultToAll={applyDefaultToAll}
        heurePersonnalise={hp}
        trentaine={trentaine}
        title={trentaine ? 'Dimanches de la trentaine' : `Horaires — ${dureeLabel}`}
      />
    </AppCard>
  );
}
