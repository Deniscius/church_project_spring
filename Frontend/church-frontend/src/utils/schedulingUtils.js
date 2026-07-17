import { WEEK_DAY_LABELS, WEEK_DAYS } from '../constants/enums';

const JS_DAY_TO_ENUM = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];

export function getDayEnumFromDateString(dateStr) {
  if (!dateStr) return null;
  const date = new Date(`${dateStr}T12:00:00`);
  if (Number.isNaN(date.getTime())) return null;
  return JS_DAY_TO_ENUM[date.getDay()];
}

export function isDateAllowedForDays(dateStr, allowedDays) {
  if (!dateStr) return false;
  if (!allowedDays?.length) return true;
  const day = getDayEnumFromDateString(dateStr);
  return day != null && allowedDays.includes(day);
}

export function formatAllowedDays(allowedDays) {
  if (!allowedDays?.length) return 'Tous les jours';
  return [...allowedDays]
    .sort((a, b) => WEEK_DAYS.indexOf(a) - WEEK_DAYS.indexOf(b))
    .map((day) => WEEK_DAY_LABELS[day] || day)
    .join(', ');
}

export function getEffectiveAllowedDays(allowedDays, horaireDay) {
  if (horaireDay) return [horaireDay];
  return allowedDays?.length ? allowedDays : WEEK_DAYS;
}

export function findNextAllowedDate(fromDateStr, allowedDays, maxLookaheadDays = 730) {
  const allowed = getEffectiveAllowedDays(allowedDays);
  const start = fromDateStr ? new Date(`${fromDateStr}T12:00:00`) : new Date();
  if (Number.isNaN(start.getTime())) return '';

  for (let offset = 0; offset <= maxLookaheadDays; offset += 1) {
    const candidate = new Date(start);
    candidate.setDate(start.getDate() + offset);
    const iso = candidate.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
    if (isDateAllowedForDays(iso, allowed)) return iso;
  }
  return '';
}
