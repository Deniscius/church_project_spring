import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppInput from '../../components/ui/AppInput';
import AppLoading from '../../components/ui/AppLoading';
import AppAlert from '../../components/ui/AppAlert';
import { useHorairesPublicActivesQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatParishTimeInUserZone, formatTime, getUserTimeZone, PARISH_TIME_ZONE } from '../../utils/formatTime';
import { WEEK_DAYS, WEEK_DAY_LABELS, WEEK_DAY_SHORT } from '../../constants/enums';
import { buildDemandePrefillPath, seedDemandeDraftFromSchedule } from '../../utils/demandePrefill';
import { daysFromParishToday, parishTodayEnum } from '../../utils/parishCalendar';

function shortDay(day) {
  return WEEK_DAY_SHORT[day] || WEEK_DAY_LABELS[day] || day || '';
}

function groupByDoyenne(parishes) {
  const map = new Map();
  for (const p of parishes) {
    const key = (p.doyenneNom || '').trim() || 'Autres paroisses';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(p);
  }
  for (const list of map.values()) {
    list.sort((a, b) => (a.paroisseNom || '').localeCompare(b.paroisseNom || '', 'fr'));
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b, 'fr'));
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
    buckets[day].sort((a, b) => String(a.heure || '').localeCompare(String(b.heure || '')));
  }
  return buckets;
}

function ParishCard({ parish, dayFilter }) {
  const byDay = useMemo(() => slotsByDay(parish.horaires), [parish.horaires]);
  // Toute la semaine : grille fixe 7 jours (cases vides si pas de créneau).
  const daysToShow = dayFilter ? WEEK_DAYS.filter((d) => d === dayFilter) : WEEK_DAYS;

  const total = daysToShow.reduce((n, d) => n + (byDay[d]?.length || 0), 0);

  return (
    <article className="schedules-parish-card">
      <header className="schedules-parish-card-head">
        <div>
          <h3 className="schedules-parish-card-title">{parish.paroisseNom}</h3>
          {parish.doyenneNom ? (
            <p className="muted schedules-parish-card-sub">{parish.doyenneNom}</p>
          ) : null}
        </div>
        <Link
          className="btn btn-secondary btn-sm"
          to={buildDemandePrefillPath()}
          onClick={() => seedDemandeDraftFromSchedule({
            paroissePublicId: parish.paroissePublicId,
            paroisseNom: parish.paroisseNom,
          })}
        >
          Demander
        </Link>
      </header>

      {total === 0 ? (
        <p className="muted schedules-empty-slots">
          {dayFilter
            ? `Aucun créneau le ${(WEEK_DAY_LABELS[dayFilter] || dayFilter).toLowerCase()}.`
            : 'Aucun créneau publié.'}
        </p>
      ) : (
        <div className={`schedules-week${dayFilter ? ' is-day-focus' : ' is-full-week'}`}>
          {daysToShow.map((day) => {
            const slots = byDay[day] || [];
            const dayLabel = dayFilter
              ? (WEEK_DAY_LABELS[day] || day)
              : shortDay(day);
            return (
              <div
                key={`${parish.paroissePublicId}-${day}`}
                className={`schedules-day-col${slots.length === 0 ? ' is-empty' : ''}`}
              >
                <h4
                  className="schedules-day-label"
                  title={dayFilter ? undefined : (WEEK_DAY_LABELS[day] || day)}
                >
                  {dayFilter ? (
                    dayLabel
                  ) : (
                    <>
                      <span className="sr-only">{WEEK_DAY_LABELS[day] || day}</span>
                      <span aria-hidden="true">{dayLabel}</span>
                    </>
                  )}
                </h4>
                {slots.length === 0 ? (
                  <p className="muted schedules-day-empty">—</p>
                ) : (
                  <ul className="schedules-slot-list">
                    {slots.map((slot, i) => {
                      const prefill = {
                        paroissePublicId: parish.paroissePublicId,
                        paroisseNom: parish.paroisseNom,
                        horairePublicId: slot.publicId,
                        horaireLibelle: [slot.heureRaw, slot.libelle].filter(Boolean).join(' · '),
                        heureCelebration: slot.heureRaw,
                        jourSemaine: slot.jourSemaine,
                        natureHonoraire: slot.natureHonoraire || '',
                      };
                      return (
                        <li key={`${day}-${slot.publicId || slot.heure}-${i}`}>
                          <Link
                            to={buildDemandePrefillPath()}
                            className="schedules-slot-link"
                            title={`Demander — ${parish.paroisseNom} ${slot.heure || ''}`}
                            onClick={() => seedDemandeDraftFromSchedule(prefill)}
                          >
                            <time className="schedules-slot-time">{slot.heure || '—'}</time>
                            <span className="schedules-slot-libelle">{slot.libelle || 'Célébration'}</span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            );
          })}
        </div>
      )}
    </article>
  );
}

function sampleRandom(items, count) {
  if (!items.length) return [];
  const pool = [...items];
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, Math.min(count, pool.length));
}

const SAMPLE_PARISH_COUNT = 8;

export default function PublicSchedulesPage() {
  const { data, isLoading, isError } = useHorairesPublicActivesQuery();
  const parishes = Array.isArray(data) ? data : [];
  const today = parishTodayEnum();
  const orderedDays = useMemo(() => daysFromParishToday(today), [today]);

  const [query, setQuery] = useState('');
  const [dayFilter, setDayFilter] = useState(today);
  const [openDoyenne, setOpenDoyenne] = useState(null);
  const [showAll, setShowAll] = useState(false);
  const [sampleSeed, setSampleSeed] = useState(0);

  const sampledParishes = useMemo(() => {
    if (showAll || parishes.length <= SAMPLE_PARISH_COUNT) return parishes;
    // sampleSeed force un nouveau tirage sans casser le memo des données.
    void sampleSeed;
    return sampleRandom(parishes, SAMPLE_PARISH_COUNT);
  }, [parishes, showAll, sampleSeed]);

  const browsingParishes = query.trim() || dayFilter || showAll ? parishes : sampledParishes;

  const byDoyenne = useMemo(() => groupByDoyenne(browsingParishes), [browsingParishes]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return byDoyenne
      .map(([doyenne, list]) => [
        doyenne,
        list.filter((p) => {
          const matchText = !q
            || (p.paroisseNom || '').toLowerCase().includes(q)
            || doyenne.toLowerCase().includes(q);
          if (!matchText) return false;
          if (!dayFilter) return true;
          return (p.horaires || []).some((h) => h.jourSemaine === dayFilter);
        }),
      ])
      .filter(([, list]) => list.length > 0);
  }, [byDoyenne, query, dayFilter]);

  const searching = query.trim().length > 0;
  const samplingActive = !searching && !dayFilter && !showAll && parishes.length > SAMPLE_PARISH_COUNT;

  useEffect(() => {
    if (!filtered.length) {
      setOpenDoyenne(null);
      return;
    }
    setOpenDoyenne((cur) => {
      if (cur && filtered.some(([name]) => name === cur)) return cur;
      return filtered[0][0];
    });
  }, [filtered]);

  const parishCount = filtered.reduce((n, [, list]) => n + list.length, 0);

  return (
    <div className="stack public-page schedules-page">
      <header className="schedules-hero">
        <p className="onboard-kicker">Missanye</p>
        <PageHeader
          title="Horaires des messes"
          subtitle={
            samplingActive
              ? `Sélection aléatoire de ${SAMPLE_PARISH_COUNT} paroisses — recherchez ou affichez tout le catalogue.`
              : 'Programmation des paroisses actives, par doyenné. Les heures s’affichent dans votre fuseau horaire.'
          }
        />
      </header>

      {getUserTimeZone() !== PARISH_TIME_ZONE ? (
        <p className="muted schedules-tz-note" style={{ marginTop: 0 }}>
          Horaires paroissiaux (Afrique/Lomé) convertis dans votre fuseau : {getUserTimeZone()}.
        </p>
      ) : null}

      <div className="schedules-toolbar">
        <label className="form-field schedules-search" htmlFor="schedules-search">
          <span className="sr-only">Rechercher un doyenné ou une paroisse</span>
          <AppInput
            id="schedules-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher un doyenné ou une paroisse…"
            autoComplete="off"
          />
        </label>
        {parishes.length > SAMPLE_PARISH_COUNT ? (
          <div className="button-row">
            {samplingActive ? (
              <>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setSampleSeed((s) => s + 1)}
                >
                  Autres paroisses
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAll(true)}
                >
                  Tout afficher ({parishes.length})
                </button>
              </>
            ) : (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setShowAll(false);
                  setSampleSeed((s) => s + 1);
                }}
              >
                Vue aléatoire
              </button>
            )}
          </div>
        ) : null}
        <Link className="btn btn-primary" to="/demande">
          Faire une demande
        </Link>
      </div>

      <div className="schedules-day-filters" role="toolbar" aria-label="Filtrer par jour">
        <button
          type="button"
          className={`schedules-day-chip${!dayFilter ? ' is-active' : ''}`}
          onClick={() => setDayFilter('')}
        >
          Toute la semaine
        </button>
        {orderedDays.map((day) => (
          <button
            key={day}
            type="button"
            className={`schedules-day-chip${dayFilter === day ? ' is-active' : ''}${day === today ? ' is-today' : ''}`}
            onClick={() => setDayFilter(day)}
          >
            {shortDay(day)}
            {day === today ? ' · auj.' : ''}
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

      {!isLoading && !isError && parishes.length > 0 && filtered.length === 0 ? (
        <p className="muted">
          Aucun résultat
          {query.trim() ? <> pour « {query.trim()} »</> : null}
          {dayFilter ? <> le {(WEEK_DAY_LABELS[dayFilter] || dayFilter).toLowerCase()}</> : null}.
        </p>
      ) : null}

      {!isLoading && filtered.length > 0 ? (
        <>
          <p className="muted schedules-summary">
            {parishCount} paroisse{parishCount > 1 ? 's' : ''}
            {dayFilter ? ` · ${WEEK_DAY_LABELS[dayFilter]}` : ''}
            {searching ? ' · résultats filtrés' : ''}
          </p>

          <div className="schedules-doyennes">
            {filtered.map(([doyenne, list]) => {
              const isOpen = searching || openDoyenne === doyenne;
              return (
                <section key={doyenne} className={`schedules-doyenne-acc${isOpen ? ' is-open' : ''}`}>
                  <button
                    type="button"
                    className="schedules-doyenne-trigger"
                    aria-expanded={isOpen}
                    onClick={() =>
                      setOpenDoyenne((cur) => (cur === doyenne && !searching ? null : doyenne))
                    }
                  >
                    <span>
                      <span className="schedules-doyenne-label">{doyenne}</span>
                      <span className="muted schedules-doyenne-count">
                        {list.length} paroisse{list.length > 1 ? 's' : ''}
                      </span>
                    </span>
                    <span className="nav-dropdown-chevron" aria-hidden="true" />
                  </button>

                  {isOpen ? (
                    <div className="schedules-parish-grid">
                      {list.map((parish) => (
                        <ParishCard
                          key={parish.paroissePublicId}
                          parish={parish}
                          dayFilter={dayFilter}
                        />
                      ))}
                    </div>
                  ) : null}
                </section>
              );
            })}
          </div>
        </>
      ) : null}
    </div>
  );
}
