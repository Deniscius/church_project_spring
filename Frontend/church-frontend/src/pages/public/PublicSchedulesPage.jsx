import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppInput from '../../components/ui/AppInput';
import AppSelect from '../../components/ui/AppSelect';
import AppLoading from '../../components/ui/AppLoading';
import AppAlert from '../../components/ui/AppAlert';
import { useHorairesPublicActivesQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatParishTimeInUserZone, formatTime, getUserTimeZone, PARISH_TIME_ZONE } from '../../utils/formatTime';
import { WEEK_DAYS, WEEK_DAY_LABELS, WEEK_DAY_SHORT } from '../../constants/enums';
import { buildDemandePrefillPath, seedDemandeDraftFromSchedule } from '../../utils/demandePrefill';
import { daysFromParishToday, parishTodayEnum } from '../../utils/parishCalendar';

const PAGE_SIZE = 10;
const EMPTY_PARISHES = [];

function shortDay(day) {
  return WEEK_DAY_SHORT[day] || WEEK_DAY_LABELS[day] || day || '';
}

function listDoyennes(parishes) {
  const names = new Set();
  for (const p of parishes) {
    const key = (p.doyenneNom || '').trim();
    if (key) names.add(key);
  }
  return [...names].sort((a, b) => a.localeCompare(b, 'fr'));
}

function slotsForDay(parish, day) {
  return (parish.horaires || [])
    .filter((slot) => !day || slot.jourSemaine === day)
    .map((slot) => ({
      heure: formatParishTimeInUserZone(slot.heureCelebration),
      heureRaw: formatTime(slot.heureCelebration) || '',
      libelle: slot.libelle || '',
      publicId: slot.publicId,
      jourSemaine: slot.jourSemaine,
      natureHonoraire: slot.natureHonoraire || '',
    }))
    .sort((a, b) => String(a.heureRaw || '').localeCompare(String(b.heureRaw || '')));
}

function slotsByDay(horaires) {
  const buckets = Object.fromEntries(WEEK_DAYS.map((d) => [d, []]));
  for (const slot of horaires || []) {
    const day = slot.jourSemaine;
    if (!day || !buckets[day]) continue;
    buckets[day].push({
      heure: formatParishTimeInUserZone(slot.heureCelebration),
      heureRaw: formatTime(slot.heureCelebration) || '',
      libelle: slot.libelle || '',
      publicId: slot.publicId,
      jourSemaine: day,
      natureHonoraire: slot.natureHonoraire || '',
    });
  }
  for (const day of WEEK_DAYS) {
    buckets[day].sort((a, b) => String(a.heureRaw || '').localeCompare(String(b.heureRaw || '')));
  }
  return buckets;
}

function CompactParishRow({ parish, dayFilter, expanded, onToggle }) {
  const daySlots = useMemo(
    () => slotsForDay(parish, dayFilter || null),
    [parish, dayFilter]
  );
  const byDay = useMemo(
    () => (expanded && !dayFilter ? slotsByDay(parish.horaires) : null),
    [expanded, dayFilter, parish.horaires]
  );

  const visibleSlots = dayFilter
    ? daySlots
    : daySlots.slice(0, expanded ? daySlots.length : 4);

  return (
    <article className={`schedules-row${expanded ? ' is-expanded' : ''}`}>
      <div className="schedules-row-main">
        <div className="schedules-row-identity">
          <h3 className="schedules-row-title">{parish.paroisseNom}</h3>
          {parish.doyenneNom ? (
            <p className="muted schedules-row-doyenne">{parish.doyenneNom}</p>
          ) : null}
        </div>

        <div className="schedules-row-times" aria-label="Horaires">
          {visibleSlots.length === 0 ? (
            <span className="muted">Aucun créneau</span>
          ) : (
            visibleSlots.map((slot, i) => {
              const prefill = {
                paroissePublicId: parish.paroissePublicId,
                paroisseNom: parish.paroisseNom,
                horairePublicId: slot.publicId,
                horaireLibelle: [slot.heureRaw, slot.libelle].filter(Boolean).join(' · '),
                heureCelebration: slot.heureRaw,
                jourSemaine: slot.jourSemaine || dayFilter,
                natureHonoraire: slot.natureHonoraire || '',
              };
              return (
                <Link
                  key={`${slot.publicId || slot.heureRaw}-${i}`}
                  to={buildDemandePrefillPath()}
                  className="schedules-time-chip"
                  title={`${slot.libelle || 'Messe'} — demander`}
                  onClick={() => seedDemandeDraftFromSchedule(prefill)}
                >
                  <time>{slot.heure || '—'}</time>
                  {!dayFilter && slot.jourSemaine ? (
                    <span className="schedules-time-day">{shortDay(slot.jourSemaine)}</span>
                  ) : null}
                </Link>
              );
            })
          )}
          {!dayFilter && !expanded && daySlots.length > 4 ? (
            <button type="button" className="schedules-more-times" onClick={onToggle}>
              +{daySlots.length - 4}
            </button>
          ) : null}
        </div>

        <div className="schedules-row-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            aria-expanded={expanded}
            onClick={onToggle}
          >
            {expanded ? 'Réduire' : (dayFilter ? 'Détail' : 'Semaine')}
          </button>
          <Link
            className="btn btn-primary btn-sm"
            to={buildDemandePrefillPath()}
            onClick={() => seedDemandeDraftFromSchedule({
              paroissePublicId: parish.paroissePublicId,
              paroisseNom: parish.paroisseNom,
            })}
          >
            Demander
          </Link>
        </div>
      </div>

      {expanded && !dayFilter && byDay ? (
        <div className="schedules-week is-full-week schedules-row-week">
          {WEEK_DAYS.map((day) => {
            const slots = byDay[day] || [];
            return (
              <div
                key={`${parish.paroissePublicId}-${day}`}
                className={`schedules-day-col${slots.length === 0 ? ' is-empty' : ''}`}
              >
                <h4 className="schedules-day-label">
                  <span className="sr-only">{WEEK_DAY_LABELS[day]}</span>
                  <span aria-hidden="true">{shortDay(day)}</span>
                </h4>
                {slots.length === 0 ? (
                  <p className="muted schedules-day-empty">—</p>
                ) : (
                  <ul className="schedules-slot-list">
                    {slots.map((slot, i) => (
                      <li key={`${day}-${slot.publicId || i}`}>
                        <Link
                          to={buildDemandePrefillPath()}
                          className="schedules-slot-link"
                          onClick={() => seedDemandeDraftFromSchedule({
                            paroissePublicId: parish.paroissePublicId,
                            paroisseNom: parish.paroisseNom,
                            horairePublicId: slot.publicId,
                            horaireLibelle: [slot.heureRaw, slot.libelle].filter(Boolean).join(' · '),
                            heureCelebration: slot.heureRaw,
                            jourSemaine: day,
                            natureHonoraire: slot.natureHonoraire || '',
                          })}
                        >
                          <time className="schedules-slot-time">{slot.heure || '—'}</time>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            );
          })}
        </div>
      ) : null}

      {expanded && dayFilter && daySlots.length > 0 ? (
        <ul className="schedules-detail-list">
          {daySlots.map((slot, i) => (
            <li key={`${slot.publicId || i}`}>
              <Link
                to={buildDemandePrefillPath()}
                className="schedules-detail-link"
                onClick={() => seedDemandeDraftFromSchedule({
                  paroissePublicId: parish.paroissePublicId,
                  paroisseNom: parish.paroisseNom,
                  horairePublicId: slot.publicId,
                  horaireLibelle: [slot.heureRaw, slot.libelle].filter(Boolean).join(' · '),
                  heureCelebration: slot.heureRaw,
                  jourSemaine: slot.jourSemaine || dayFilter,
                  natureHonoraire: slot.natureHonoraire || '',
                })}
              >
                <time>{slot.heure || '—'}</time>
                <span>{slot.libelle || 'Célébration'}</span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </article>
  );
}

export default function PublicSchedulesPage() {
  const { data, isLoading, isError } = useHorairesPublicActivesQuery();
  const parishes = Array.isArray(data) ? data : EMPTY_PARISHES;
  const today = parishTodayEnum();
  const orderedDays = useMemo(() => daysFromParishToday(today), [today]);
  const doyennes = useMemo(() => listDoyennes(parishes), [parishes]);

  const [query, setQuery] = useState('');
  const [dayFilter, setDayFilter] = useState(today);
  const [doyenneFilter, setDoyenneFilter] = useState('');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [expandedId, setExpandedId] = useState(null);

  const doyenneOptions = useMemo(
    () => doyennes.map((name) => ({ value: name, label: name })),
    [doyennes]
  );

  const filteredParishes = useMemo(() => {
    const q = query.trim().toLowerCase();
    return parishes
      .filter((p) => {
        if (doyenneFilter) {
          const d = (p.doyenneNom || '').trim();
          if (d !== doyenneFilter) return false;
        }
        if (q) {
          const hay = `${p.paroisseNom || ''} ${p.doyenneNom || ''}`.toLowerCase();
          if (!hay.includes(q)) return false;
        }
        if (dayFilter) {
          return (p.horaires || []).some((h) => h.jourSemaine === dayFilter);
        }
        return (p.horaires || []).length > 0;
      })
      .sort((a, b) => (a.paroisseNom || '').localeCompare(b.paroisseNom || '', 'fr'));
  }, [parishes, query, dayFilter, doyenneFilter]);

  const filterKey = `${query}|${dayFilter}|${doyenneFilter}`;
  const [pageKey, setPageKey] = useState(filterKey);
  if (filterKey !== pageKey) {
    setPageKey(filterKey);
    setVisibleCount(PAGE_SIZE);
    setExpandedId(null);
  }

  const visibleParishes = filteredParishes.slice(0, visibleCount);
  const hasMore = filteredParishes.length > visibleCount;
  const remaining = filteredParishes.length - visibleCount;

  return (
    <div className="stack public-page schedules-page">
      <header className="schedules-hero">
        <p className="onboard-kicker">Missanye</p>
        <PageHeader
          title="Horaires des messes"
          subtitle="Cherchez votre paroisse, choisissez un jour, puis cliquez une heure pour démarrer une demande."
        />
      </header>

      {getUserTimeZone() !== PARISH_TIME_ZONE ? (
        <p className="muted schedules-tz-note" style={{ marginTop: 0 }}>
          Horaires convertis dans votre fuseau : {getUserTimeZone()}.
        </p>
      ) : null}

      <div className="schedules-find">
        <label className="form-field schedules-search" htmlFor="schedules-search">
          <span className="schedules-filter-label">Paroisse</span>
          <AppInput
            id="schedules-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Ex. Maria Goretti, Adidogomé…"
            autoComplete="off"
          />
        </label>

        {doyennes.length > 1 ? (
          <div className="form-field schedules-doyenne-filter">
            <span className="schedules-filter-label" id="schedules-doyenne-label">Doyenné</span>
            <AppSelect
              id="schedules-doyenne"
              aria-labelledby="schedules-doyenne-label"
              value={doyenneFilter}
              options={doyenneOptions}
              placeholder="Tous les doyennés"
              searchable={doyennes.length > 6}
              searchPlaceholder="Filtrer un doyenné…"
              onChange={(value) => setDoyenneFilter(value || '')}
            />
          </div>
        ) : null}

        <div className="schedules-find-cta">
          <Link className="btn btn-primary" to="/demande">
            Faire une demande
          </Link>
        </div>
      </div>

      <div className="schedules-day-filters" role="toolbar" aria-label="Filtrer par jour">
        <button
          type="button"
          className={`schedules-day-chip${!dayFilter ? ' is-active' : ''}`}
          onClick={() => setDayFilter('')}
        >
          Semaine
        </button>
        {orderedDays.map((day) => (
          <button
            key={day}
            type="button"
            className={`schedules-day-chip${dayFilter === day ? ' is-active' : ''}${day === today ? ' is-today' : ''}`}
            onClick={() => setDayFilter(day)}
          >
            {day === today ? 'Aujourd’hui' : shortDay(day)}
          </button>
        ))}
      </div>

      {isLoading ? <AppLoading message="Chargement des horaires…" /> : null}

      {!isLoading && isError ? (
        <AppAlert variant="danger">Impossible de charger les horaires pour le moment.</AppAlert>
      ) : null}

      {!isLoading && !isError && parishes.length === 0 ? (
        <p className="muted">Aucune paroisse active pour le moment.</p>
      ) : null}

      {!isLoading && !isError && parishes.length > 0 && filteredParishes.length === 0 ? (
        <div className="schedules-empty">
          <p className="muted" style={{ margin: 0 }}>
            Aucune paroisse ne correspond
            {query.trim() ? <> à « {query.trim()} »</> : null}
            {doyenneFilter ? <> dans {doyenneFilter}</> : null}
            {dayFilter ? <> pour {(WEEK_DAY_LABELS[dayFilter] || dayFilter).toLowerCase()}</> : null}.
          </p>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setQuery('');
              setDoyenneFilter('');
              setDayFilter(today);
            }}
          >
            Réinitialiser les filtres
          </button>
        </div>
      ) : null}

      {!isLoading && filteredParishes.length > 0 ? (
        <>
          <p className="muted schedules-summary">
            {filteredParishes.length} paroisse{filteredParishes.length > 1 ? 's' : ''}
            {dayFilter ? ` · ${dayFilter === today ? 'aujourd’hui' : WEEK_DAY_LABELS[dayFilter]}` : ' · semaine'}
            {doyenneFilter ? ` · ${doyenneFilter}` : ''}
            {visibleParishes.length < filteredParishes.length
              ? ` · affichage de ${visibleParishes.length}`
              : ''}
          </p>

          <div className="schedules-list" role="list">
            {visibleParishes.map((parish) => (
              <div key={parish.paroissePublicId} role="listitem">
                <CompactParishRow
                  parish={parish}
                  dayFilter={dayFilter}
                  expanded={expandedId === parish.paroissePublicId}
                  onToggle={() => setExpandedId((cur) => (
                    cur === parish.paroissePublicId ? null : parish.paroissePublicId
                  ))}
                />
              </div>
            ))}
          </div>

          {hasMore ? (
            <div className="schedules-load-more">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setVisibleCount((n) => n + PAGE_SIZE)}
              >
                Voir plus ({remaining} restante{remaining > 1 ? 's' : ''})
              </button>
            </div>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
