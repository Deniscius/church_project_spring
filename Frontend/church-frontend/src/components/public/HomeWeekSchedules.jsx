import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useHorairesPublicActivesQuery } from '../../hooks/queries/usePublicReferentiel';
import { formatTime } from '../../utils/formatTime';
import { WEEK_DAYS, WEEK_DAY_LABELS } from '../../constants/enums';

const SAMPLE_SIZE = 5;
const ROTATE_MS = 4500;

function todayEnum() {
  const map = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];
  return map[new Date().getDay()];
}

/** Tirage aléatoire sans remise, ordre mélangé. */
function sampleRandom(items, count) {
  if (!items.length) return [];
  const pool = [...items];
  for (let i = pool.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, Math.min(count, pool.length));
}

function slotsForDay(parish, day) {
  return (parish.horaires || [])
    .filter((slot) => slot.jourSemaine === day)
    .map((slot) => ({
      heure: formatTime(slot.heureCelebration),
      libelle: slot.libelle || '',
      jourSemaine: slot.jourSemaine,
    }))
    .sort((a, b) => String(a.heure || '').localeCompare(String(b.heure || '')));
}

/**
 * Accueil : quelques paroisses tirées au hasard, programmes en défilement.
 * Le catalogue complet reste sur /horaires.
 */
export default function HomeWeekSchedules() {
  const { data, isLoading, isError } = useHorairesPublicActivesQuery();
  const today = todayEnum();

  const sampled = useMemo(() => {
    const list = Array.isArray(data) ? data : [];
    const withSlots = list.filter((p) => (p.horaires || []).length > 0);
    return sampleRandom(withSlots, SAMPLE_SIZE);
  }, [data]);

  const programmes = useMemo(() => {
    const cards = [];
    for (const parish of sampled) {
      let slots = slotsForDay(parish, today);
      let dayLabel = WEEK_DAY_LABELS[today] || today;
      let isToday = true;

      if (slots.length === 0) {
        // Si rien aujourd'hui, prendre le prochain jour avec horaire (ordre semaine).
        const startIdx = WEEK_DAYS.indexOf(today);
        const ordered = [...WEEK_DAYS.slice(startIdx), ...WEEK_DAYS.slice(0, startIdx)];
        for (const day of ordered) {
          const daySlots = slotsForDay(parish, day);
          if (daySlots.length) {
            slots = daySlots;
            dayLabel = WEEK_DAY_LABELS[day] || day;
            isToday = day === today;
            break;
          }
        }
      }

      if (!slots.length) continue;

      cards.push({
        paroissePublicId: parish.paroissePublicId,
        paroisseNom: parish.paroisseNom,
        doyenneNom: parish.doyenneNom || '',
        dayLabel,
        isToday,
        slots: sampleRandom(slots, Math.min(4, slots.length)),
      });
    }
    return sampleRandom(cards, cards.length);
  }, [sampled, today]);

  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    setActiveIndex(0);
  }, [programmes]);

  useEffect(() => {
    if (programmes.length <= 1) return undefined;
    const id = window.setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % programmes.length);
    }, ROTATE_MS);
    return () => window.clearInterval(id);
  }, [programmes]);

  const active = programmes[activeIndex] || null;

  return (
    <section className="home-section home-section-schedules container" aria-labelledby="home-schedules-title">
      <div className="home-schedules">
        <div className="home-schedules-head home-section-head">
          <h2 id="home-schedules-title">Horaires en ce moment</h2>
          <p className="muted">
            Aperçu tournant de quelques paroisses — le détail complet est sur la page horaires.
          </p>
        </div>

        {isLoading ? <p className="muted">Chargement des horaires…</p> : null}
        {isError ? <p className="muted">Impossible de charger les horaires pour le moment.</p> : null}

        {!isLoading && !isError && programmes.length === 0 ? (
          <p className="muted">Aucun horaire publié pour l’instant.</p>
        ) : null}

        {!isLoading && !isError && active ? (
          <>
            <div
              className="home-schedule-rotator"
              aria-live="polite"
              aria-atomic="true"
            >
              <article className="home-schedule-card" key={active.paroissePublicId}>
                <header className="home-schedule-card-head">
                  <div>
                    <h3 className="home-schedule-parish">{active.paroisseNom}</h3>
                    {active.doyenneNom ? (
                      <p className="muted home-schedule-doyenne">{active.doyenneNom}</p>
                    ) : null}
                  </div>
                  <span className={`home-day-badge${active.isToday ? '' : ' home-day-badge--soon'}`}>
                    {active.isToday ? 'Aujourd’hui' : active.dayLabel}
                  </span>
                </header>

                <ul className="home-schedule-slots">
                  {active.slots.map((slot, i) => (
                    <li key={`${active.paroissePublicId}-${slot.heure}-${i}`}>
                      <time className="home-day-time" dateTime={slot.heure || undefined}>
                        {slot.heure || '—'}
                      </time>
                      <span className="home-day-libelle">{slot.libelle || 'Célébration'}</span>
                    </li>
                  ))}
                </ul>
              </article>

              {programmes.length > 1 ? (
                <div className="home-schedule-dots" role="tablist" aria-label="Paroisses affichées">
                  {programmes.map((p, i) => (
                    <button
                      key={p.paroissePublicId}
                      type="button"
                      role="tab"
                      aria-selected={i === activeIndex}
                      aria-label={p.paroisseNom}
                      className={`home-schedule-dot${i === activeIndex ? ' is-active' : ''}`}
                      onClick={() => setActiveIndex(i)}
                    />
                  ))}
                </div>
              ) : null}
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
