/**
 * Normalise une heure renvoyée par l'API (`LocalTime` sérialisé « HH:mm:ss »)
 * en libellé court « HH:mm ».
 */

/** Fuseau métier des paroisses (horaires stockés sans offset). */
export const PARISH_TIME_ZONE = 'Africa/Lome';

export function formatTime(value) {
  if (!value) return null;
  const raw = String(value).trim();
  const match = raw.match(/^(\d{1,2}):(\d{2})/);
  if (!match) return raw;
  return `${match[1].padStart(2, '0')}:${match[2]}`;
}

export function getUserTimeZone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || PARISH_TIME_ZONE;
  } catch {
    return PARISH_TIME_ZONE;
  }
}

/**
 * Convertit une heure paroissiale (Africa/Lome) vers le fuseau du navigateur.
 * Retourne { parish, local, sameZone, label }.
 */
export function convertParishTimeToUser(value, referenceDate = new Date()) {
  const parish = formatTime(value);
  if (!parish) {
    return { parish: null, local: null, sameZone: true, label: null };
  }

  const userTz = getUserTimeZone();
  const sameZone = userTz === PARISH_TIME_ZONE;
  if (sameZone) {
    return { parish, local: parish, sameZone: true, label: parish };
  }

  const dateIso = referenceDate.toLocaleDateString('en-CA', { timeZone: PARISH_TIME_ZONE });
  // Africa/Lome = UTC+0 toute l'année.
  const instant = new Date(`${dateIso}T${parish}:00+00:00`);
  if (Number.isNaN(instant.getTime())) {
    return { parish, local: parish, sameZone: true, label: parish };
  }

  const local = instant.toLocaleTimeString('fr-FR', {
    timeZone: userTz,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

  return {
    parish,
    local,
    sameZone: false,
    label: local === parish ? parish : `${parish} · ${local} chez vous`,
  };
}

/** Affiche l'heure paroissiale adaptée au fuseau de l'utilisateur. */
export function formatParishTimeInUserZone(value, referenceDate = new Date()) {
  return convertParishTimeToUser(value, referenceDate).label;
}

/**
 * Heure effective d'une demande : l'heure personnalisée prime sur l'horaire
 * paroissial. Le libellé de l'horaire n'est qu'un complément d'affichage.
 */
export function formatCelebrationTime(demande) {
  if (!demande) return '—';

  const custom = formatParishTimeInUserZone(demande.heurePersonnalisee);
  if (custom) return custom;

  const scheduled = formatParishTimeInUserZone(demande.horaireHeure);
  if (scheduled) {
    return demande.horaireLibelle ? `${scheduled} · ${demande.horaireLibelle}` : scheduled;
  }

  return demande.horaireLibelle || '—';
}
