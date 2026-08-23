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

export function resolveAllowedDays(typeDays = [], forfaitDays = []) {
  if (forfaitDays?.length) {
    if (typeDays?.length) {
      const intersection = forfaitDays.filter((day) => typeDays.includes(day));
      return intersection.length ? intersection : forfaitDays;
    }
    return forfaitDays;
  }
  if (typeDays?.length) return typeDays;
  return WEEK_DAYS;
}

export function getEffectiveAllowedDays(typeDays, forfaitDays, horaireDay) {
  const resolved = resolveAllowedDays(typeDays, forfaitDays);
  if (!horaireDay) return resolved;
  // Horaire déjà choisi : on ne garde ce jour que s'il reste compatible avec le forfait/type.
  return resolved.includes(horaireDay) ? [horaireDay] : [];
}

export function filterDaysWithinType(typeDays = [], selectedDays = []) {
  if (!typeDays.length) return selectedDays;
  return selectedDays.filter((day) => typeDays.includes(day));
}

export function findNextAllowedDate(fromDateStr, allowedDays, maxLookaheadDays = 730) {
  const allowed = allowedDays?.length ? allowedDays : WEEK_DAYS;
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

/**
 * Date minimale de célébration compte tenu du délai (fuseau Lomé).
 */
export function getMinimumCelebrationDateIso(delaiMinimumHeures = 24) {
  const ms = (Number(delaiMinimumHeures) || 24) * 3_600_000;
  return new Date(Date.now() + ms).toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
}

/**
 * Si le programme paroissial contient une date précise (événement solennel / messe unique)
 * compatible avec le délai → cette date.
 * Les dates précises du programme ne sont pas limitées aux jours habituels du type
 * (l’admin peut programmer une solennité n’importe quel jour).
 */
export function resolveReferencedCelebrationDate({
  delaiMinimumHeures = 24,
  horaires = [],
} = {}) {
  if (!Array.isArray(horaires) || !horaires.length) return '';

  const minIso = getMinimumCelebrationDateIso(delaiMinimumHeures);

  const candidates = horaires
    .filter((h) => h && h.dateSpecifique && h.isActive !== false && !h.statusDel)
    .filter((h) => h.dateSpecifique >= minIso)
    .sort((a, b) => {
      const uniqueRank = Number(Boolean(b.uniqueSurParoisse)) - Number(Boolean(a.uniqueSurParoisse));
      if (uniqueRank) return uniqueRank;
      const solemnRank = Number(b.natureHonoraire === 'SPECIALE') - Number(a.natureHonoraire === 'SPECIALE');
      if (solemnRank) return solemnRank;
      return String(a.dateSpecifique).localeCompare(String(b.dateSpecifique));
    });

  return candidates[0]?.dateSpecifique || '';
}

/** Honoraire publié sur un créneau à date précise (sinon null). */
export function resolveNatureHonoraireForDate(horaires, dateStr) {
  if (!dateStr || !Array.isArray(horaires)) return null;
  const oneOffs = horaires.filter(
    (h) =>
      h
      && h.dateSpecifique === dateStr
      && h.isActive !== false
      && !h.statusDel
  );
  if (!oneOffs.length) return null;
  const unique = oneOffs.find((h) => h.uniqueSurParoisse);
  const slot = unique || oneOffs[0];
  return slot?.natureHonoraire || null;
}

/** True si la paroisse a au moins un créneau ponctuel (date précise) ce jour-là. */
export function isDateSpecifiqueDay(horaires, dateStr) {
  if (!dateStr || !Array.isArray(horaires)) return false;
  return horaires.some(
    (h) =>
      h
      && h.dateSpecifique === dateStr
      && h.isActive !== false
      && !h.statusDel
  );
}

/**
 * Une demande « date précise » du programme : une seule célébration
 * sur un jour où la paroisse a publié un créneau ponctuel.
 * (Les triduum / neuvaines peuvent seulement « tomber » sur ce jour, pas le cibler seuls.)
 */
export function isDatePrecisDemandeContext(horaires, dateDebut, nombreCelebration) {
  if (!dateDebut || isMultiCelebrationForfaitNumber(nombreCelebration)) return false;
  return isDateSpecifiqueDay(horaires, dateDebut);
}

function isMultiCelebrationForfaitNumber(nombreCelebration) {
  return nombreCelebration != null && Number(nombreCelebration) > 1;
}

/** Dates d'une série multi qui coïncident avec une date précise du programme. */
export function findDateSpecifiqueInSeries(horaires, dates = []) {
  if (!Array.isArray(dates) || !dates.length) return [];
  return dates.filter((iso) => isDateSpecifiqueDay(horaires, iso));
}



/**
 * Liste les prochaines dates correspondant aux jours autorisés.
 * @param {string} fromDateStr ISO yyyy-mm-dd
 * @param {string[]} allowedDays enums JourSemaine
 * @param {{ count?: number, untilIso?: string|null }} options
 */
export function listUpcomingAllowedDates(fromDateStr, allowedDays, options = {}) {
  const count = options.count ?? 16;
  const untilIso = options.untilIso || null;
  const allowed = allowedDays?.length ? allowedDays : WEEK_DAYS;
  const start = fromDateStr ? new Date(`${fromDateStr}T12:00:00`) : new Date();
  if (Number.isNaN(start.getTime())) return [];

  const out = [];
  for (let offset = 0; offset <= 800 && out.length < count; offset += 1) {
    const candidate = new Date(start);
    candidate.setDate(start.getDate() + offset);
    const iso = candidate.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
    if (untilIso && iso > untilIso) break;
    if (isDateAllowedForDays(iso, allowed)) out.push(iso);
  }
  return out;
}

/**
 * Génère N dates de célébration à partir d'une date de début,
 * en ne retenant que les jours autorisés (aligné sur DemandeSchedulingPolicy).
 */
export function computeCelebrationDates(startDateStr, allowedDays, nombreCelebrations) {
  const count = Number(nombreCelebrations) || 0;
  if (!startDateStr || count <= 0) return [];
  // Trentaine : 30 jours calendaires successifs (sans sauter de jour).
  if (count === 30) {
    return computeConsecutiveCalendarDates(startDateStr, count);
  }
  if (!isDateAllowedForDays(startDateStr, allowedDays)) return [];

  const dates = [];
  const cursor = new Date(`${startDateStr}T12:00:00`);
  if (Number.isNaN(cursor.getTime())) return [];

  const max = new Date(cursor);
  max.setFullYear(max.getFullYear() + 2);

  while (dates.length < count) {
    const iso = cursor.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
    if (isDateAllowedForDays(iso, allowedDays)) {
      dates.push(iso);
    }
    cursor.setDate(cursor.getDate() + 1);
    if (cursor > max) break;
  }
  return dates.length === count ? dates : [];
}

/** True pour une trentaine (30 célébrations journalières successives). */
export function isTrentaineForfait(nombreCelebrations) {
  return Number(nombreCelebrations) === 30;
}

/**
 * Filtre les horaires effectivement au programme pour une date ISO,
 * en tenant compte des créneaux ponctuels et de la messe unique.
 * Aligné sur HoraireServiceImpl#buildProgrammeDay.
 */
export function isCelebrationSlotAvailable(dateStr, timeStr, now = new Date()) {
  if (!dateStr || !timeStr) return false;
  const today = now.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
  if (dateStr > today) return true;
  if (dateStr < today) return false;
  const currentTime = now.toLocaleTimeString('en-GB', {
    timeZone: 'Africa/Lome',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).slice(0, 5);
  return String(timeStr).slice(0, 5) > currentTime;
}

export function resolveHorairesForDate(horaires, dateStr) {
  if (!dateStr || !Array.isArray(horaires)) return [];
  const day = getDayEnumFromDateString(dateStr);
  if (!day) return [];

  const active = horaires.filter((h) => h && h.isActive !== false && !h.statusDel);
  const oneOffs = active.filter((h) => h.dateSpecifique === dateStr);
  const uniqueOnes = oneOffs.filter((h) => h.uniqueSurParoisse === true);

  if (uniqueOnes.length) return uniqueOnes;

  const weekly = active.filter(
    (h) => !h.dateSpecifique && (!h.jourSemaine || h.jourSemaine === day)
  );
  return [...weekly, ...oneOffs];
}

/** True si une messe unique est au programme ce jour-là. */
export function isMesseUniqueDay(horaires, dateStr) {
  const slots = resolveHorairesForDate(horaires, dateStr);
  return slots.length > 0 && slots.every((h) => h.uniqueSurParoisse === true);
}


/** N jours calendaires consécutifs (sans sauter de jour). */
export function computeConsecutiveCalendarDates(startDateStr, nombreCelebrations) {
  const count = Number(nombreCelebrations) || 0;
  if (!startDateStr || count <= 0) return [];
  const cursor = new Date(`${startDateStr}T12:00:00`);
  if (Number.isNaN(cursor.getTime())) return [];
  const dates = [];
  for (let i = 0; i < count; i += 1) {
    const d = new Date(cursor);
    d.setDate(cursor.getDate() + i);
    dates.push(d.toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' }));
  }
  return dates;
}

export function horairesForDate(horaires, dateIso) {
  if (!dateIso || !horaires?.length) return [];
  const day = getDayEnumFromDateString(dateIso);
  if (!day) return [];
  return horaires.filter((h) => !h.jourSemaine || h.jourSemaine === day);
}

export function emptySchedule() {
  return {
    horairePublicId: '',
    horaireLibelle: '',
    heureCelebration: '',
    jourSemaine: '',
    heurePersonnalisee: '',
  };
}

export function emptyCelebrationSlot() {
  return {
    date: '',
    horairePublicId: '',
    horaireLibelle: '',
    heureCelebration: '',
    heurePersonnalisee: '',
  };
}
