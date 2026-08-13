import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useHorairesPublicActivesQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatParishTimeInUserZone, formatTime, compareTimeAsc } from '../../utils/formatTime';
import { WEEK_DAYS, WEEK_DAY_LABELS, WEEK_DAY_SHORT } from '../../constants/enums';
import { seedDemandeDraftFromSchedule } from '../../utils/demandePrefill';
import {
  msUntilParishMidnight,
  parishTodayEnum,
  parishUpcomingWeek,
} from '../../utils/parishCalendar';

const SAMPLE_SIZE = 6;
const RESAMPLE_MS = 12000;

function sampleRandom(items, count) {
  if (!items.length) return [];
  const pool = [...items];
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, Math.min(count, pool.length));
}

function slotsForDay(parish, day, iso) {
  const dayIso = iso ? String(iso).slice(0, 10) : '';
  return (parish.horaires || [])
    .filter((slot) => {
      // Programme résolu de la semaine : filtre sur la date calendaire.
      if (slot.date && dayIso) {
        return String(slot.date).slice(0, 10) === dayIso;
      }
      return slot.jourSemaine === day;
    })
    .map((slot) => ({
      publicId: slot.publicId || '',
      heure: formatParishTimeInUserZone(slot.heureCelebration),
      heureRaw: formatTime(slot.heureCelebration) || '',
      libelle: slot.libelle || '',
      jourSemaine: slot.jourSemaine,
      date: slot.date ? String(slot.date).slice(0, 10) : dayIso,
      natureHonoraire: slot.natureHonoraire || '',
      dateSpecifique: Boolean(slot.dateSpecifique),
      uniqueSurParoisse: Boolean(slot.uniqueSurParoisse),
    }))
    .sort((a, b) => compareTimeAsc(a.heureRaw, b.heureRaw));
}

/**
 * Accueil : onglets jours + paroisses aléatoires qui se renouvellent.
 * Clic créneau → préremplit paroisse / heure / date.
 */
export default function HomeWeekSchedules() {
  const [sectionVisible, setSectionVisible] = useState(false);
  const { data, isLoading, isError } = useHorairesPublicActivesQuery({
    enabled: sectionVisible,
  });
  const [weekTick, setWeekTick] = useState(0);
  const weekDays = useMemo(() => {
    void weekTick;
    return parishUpcomingWeek();
  }, [weekTick]);
  const today = weekDays[0]?.day || parishTodayEnum();
  const [selectedDay, setSelectedDay] = useState(today);
  const [sampleSeed, setSampleSeed] = useState(0);

  // Recalcule la semaine calendaire (Lomé) à chaque minuit paroissial.
  useEffect(() => {
    let timer;
    let prevToday = parishTodayEnum();
    const tick = () => {
      const next = parishTodayEnum();
      setWeekTick((n) => n + 1);
      if (next !== prevToday) {
        setSelectedDay((sel) => (sel === prevToday ? next : sel));
        prevToday = next;
      }
      timer = window.setTimeout(tick, msUntilParishMidnight());
    };
    timer = window.setTimeout(tick, msUntilParishMidnight());
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (typeof IntersectionObserver === 'undefined') {
      const t = window.setTimeout(() => setSectionVisible(true), 0);
      return () => window.clearTimeout(t);
    }
    const el = document.getElementById('home-schedules-anchor');
    if (!el) {
      const t = window.setTimeout(() => setSectionVisible(true), 0);
      return () => window.clearTimeout(t);
    }
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setSectionVisible(true);
          obs.disconnect();
        }
      },
      { rootMargin: '120px' }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  const allParishes = useMemo(
    () => (Array.isArray(data) ? data : []).filter((p) => (p.horaires || []).length > 0),
    [data]
  );

  const dayCounts = useMemo(() => {
    const counts = Object.fromEntries(WEEK_DAYS.map((d) => [d, 0]));
    for (const parish of allParishes) {
      for (const { day, iso } of weekDays) {
        if (slotsForDay(parish, day, iso).length) counts[day] += 1;
      }
    }
    return counts;
  }, [allParishes, weekDays]);

  const programmes = useMemo(() => {
    void sampleSeed;
    const selectedIso = weekDays.find((w) => w.day === selectedDay)?.iso || '';
    const withDay = allParishes.filter((p) => slotsForDay(p, selectedDay, selectedIso).length > 0);
    const sampled = sampleRandom(withDay, SAMPLE_SIZE);
    return sampled.map((parish) => ({
      paroissePublicId: parish.paroissePublicId,
      paroisseNom: parish.paroisseNom,
      doyenneNom: parish.doyenneNom || '',
      // Programme réel du jour pour cette paroisse (hebdo + ponctuels), ordre chrono.
      slots: slotsForDay(parish, selectedDay, selectedIso),
    }));
  }, [allParishes, selectedDay, sampleSeed, weekDays]);

  const selectedMeta = weekDays.find((w) => w.day === selectedDay);

  useEffect(() => {
    if (programmes.length <= 1) return undefined;
    const id = window.setInterval(() => {
      setSampleSeed((s) => s + 1);
    }, RESAMPLE_MS);
    return () => window.clearInterval(id);
  }, [programmes.length, selectedDay]);

  return (
    <section
      id="home-schedules-anchor"
      className="home-section home-section-schedules container"
      aria-labelledby="home-schedules-title"
    >
      <div className="home-schedules">
        <div className="home-schedules-head home-section-head">
          <h2 id="home-schedules-title">Horaires en ce moment</h2>
          <p className="muted">
            Programme de la semaine en cours : des paroisses s’affichent au hasard, avec leurs
            horaires du jour (y compris les messes ponctuelles), dans l’ordre. Un clic préremplit
            votre demande.
          </p>
        </div>

        {!sectionVisible || isLoading ? <p className="muted">Chargement des horaires…</p> : null}
        {isError ? (
          <p className="muted">
            Impossible de charger les horaires pour le moment. Réessayez dans un instant.
          </p>
        ) : null}

        {sectionVisible && !isLoading && !isError ? (
          <>
            <div className="home-day-tabs" role="tablist" aria-label="Jours de la semaine">
              {weekDays.map(({ day, dayNum, monthLabel, iso }) => {
                const count = dayCounts[day] || 0;
                const isToday = day === today;
                const short = WEEK_DAY_SHORT[day] || WEEK_DAY_LABELS[day] || day;
                return (
                  <button
                    key={iso}
                    type="button"
                    role="tab"
                    aria-selected={selectedDay === day}
                    aria-label={`${WEEK_DAY_LABELS[day] || day} ${dayNum} ${monthLabel}${count ? `, ${count} paroisse${count > 1 ? 's' : ''}` : ''}`}
                    title={count ? `${count} paroisse${count > 1 ? 's' : ''} avec créneau` : 'Aucun créneau'}
                    className={`home-day-tab${selectedDay === day ? ' is-selected' : ''}${isToday ? ' is-today' : ''}`}
                    onClick={() => setSelectedDay(day)}
                  >
                    <span className="home-day-tab-label">{short}</span>
                    <span className="home-day-tab-date">
                      <strong>{dayNum}</strong>
                      <span className="home-day-tab-month">{monthLabel}</span>
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="home-day-panel" key={`${selectedDay}-${sampleSeed}`}>
              <header className="home-day-panel-head">
                <h3>
                  {WEEK_DAY_LABELS[selectedDay] || selectedDay}
                  {selectedMeta ? (
                    <span className="home-day-panel-date">
                      {selectedMeta.dayNum} {selectedMeta.monthLabel}
                    </span>
                  ) : null}
                  {selectedDay === today ? <span className="home-day-badge">Aujourd’hui</span> : null}
                </h3>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setSampleSeed((s) => s + 1)}
                  disabled={!allParishes.length}
                >
                  Autres paroisses
                </button>
              </header>

              {programmes.length === 0 ? (
                <p className="muted">Aucun créneau publié pour ce jour.</p>
              ) : (
                <div className="home-day-timeline">
                  {programmes.map((parish) => (
                    <article key={parish.paroissePublicId} className="home-schedule-card home-day-parish">
                      <header className="home-schedule-card-head">
                        <div>
                          <h4 className="home-schedule-parish">{parish.paroisseNom}</h4>
                          {parish.doyenneNom ? (
                            <p className="muted home-schedule-doyenne">{parish.doyenneNom}</p>
                          ) : null}
                        </div>
                      </header>
                      <ul className="home-schedule-slots">
                        {parish.slots.map((slot, i) => (
                            <li key={`${parish.paroissePublicId}-${slot.heureRaw}-${i}`}>
                              <Link
                                to="/demande"
                                className="home-schedule-slot-link"
                                onClick={() => seedDemandeDraftFromSchedule({
                                  paroissePublicId: parish.paroissePublicId,
                                  paroisseNom: parish.paroisseNom,
                                  horairePublicId: slot.publicId,
                                  horaireLibelle: [slot.heureRaw, slot.libelle].filter(Boolean).join(' · '),
                                  heureCelebration: slot.heureRaw,
                                  jourSemaine: slot.jourSemaine || selectedDay,
                                  dateIso: slot.date || selectedMeta?.iso || '',
                                  natureHonoraire: slot.natureHonoraire || '',
                                })}
                              >
                                <time className="home-day-time" dateTime={slot.heureRaw || undefined}>
                                  {slot.heure || '—'}
                                </time>
                                <span className="home-day-libelle">{slot.libelle || 'Célébration'}</span>
                              </Link>
                            </li>
                          ))}
                      </ul>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <div className="button-row home-schedules-actions">
              <Link className="btn btn-secondary" to="/horaires">
                Voir toutes les paroisses
              </Link>
              <Link className="btn btn-primary" to="/demande">
                Faire une demande
              </Link>
            </div>
          </>
        ) : null}
      </div>
    </section>
  );
}
