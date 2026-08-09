import { PARISH_TIME_ZONE } from './formatTime';
import { WEEK_DAYS } from '../constants/enums';

/**
 * Jour de la semaine « métier » (Africa/Lomé), pas le fuseau du navigateur.
 * Aligné sur schedulingUtils / horaires paroissiaux.
 */
export function parishTodayEnum(now = new Date()) {
  const weekday = new Intl.DateTimeFormat('en-US', {
    timeZone: PARISH_TIME_ZONE,
    weekday: 'short',
  }).format(now);
  const map = {
    Sun: 'DIMANCHE',
    Mon: 'LUNDI',
    Tue: 'MARDI',
    Wed: 'MERCREDI',
    Thu: 'JEUDI',
    Fri: 'VENDREDI',
    Sat: 'SAMEDI',
  };
  return map[weekday] || WEEK_DAYS[0];
}

/** Date ISO (yyyy-mm-dd) au fuseau paroissial. */
export function parishTodayIso(now = new Date()) {
  return now.toLocaleDateString('en-CA', { timeZone: PARISH_TIME_ZONE });
}

/**
 * Jours de la semaine ordonnés à partir d’aujourd’hui (Lomé).
 */
export function daysFromParishToday(today = parishTodayEnum()) {
  const index = WEEK_DAYS.indexOf(today);
  if (index < 0) return [...WEEK_DAYS];
  return [...WEEK_DAYS.slice(index), ...WEEK_DAYS.slice(0, index)];
}

/**
 * Les 7 prochains jours calendaires depuis « aujourd’hui » (Lomé),
 * alignés sur daysFromParishToday.
 * @returns {{ day: string, iso: string, dayNum: number, monthLabel: string }[]}
 */
export function parishUpcomingWeek(now = new Date()) {
  const today = parishTodayEnum(now);
  const ordered = daysFromParishToday(today);
  const todayIso = parishTodayIso(now);
  const [y, m, d] = todayIso.split('-').map(Number);

  return ordered.map((day, offset) => {
    // Midi UTC : Lomé = UTC+0, évite les décalages de bordure de jour.
    const utc = new Date(Date.UTC(y, m - 1, d + offset, 12, 0, 0));
    const iso = utc.toISOString().slice(0, 10);
    const dayNum = utc.getUTCDate();
    const monthLabel = new Intl.DateTimeFormat('fr-FR', {
      month: 'short',
      timeZone: 'UTC',
    }).format(utc);
    return { day, iso, dayNum, monthLabel };
  });
}

/**
 * ms jusqu’au prochain changement de jour à Lomé (+ petite marge).
 */
export function msUntilParishMidnight(now = new Date()) {
  const iso = parishTodayIso(now);
  const [y, m, d] = iso.split('-').map(Number);
  // Lomé = UTC+0 : minuit suivant = lendemain 00:00Z
  const next = Date.UTC(y, m - 1, d + 1, 0, 0, 5);
  return Math.max(1000, next - now.getTime());
}
