/**
 * Normalise une heure renvoyée par l'API (`LocalTime` sérialisé « HH:mm:ss »)
 * en libellé court « HH:mm ».
 */
export function formatTime(value) {
  if (!value) return null;
  const raw = String(value).trim();
  const match = raw.match(/^(\d{1,2}):(\d{2})/);
  if (!match) return raw;
  return `${match[1].padStart(2, '0')}:${match[2]}`;
}

/**
 * Heure effective d'une demande : l'heure personnalisée prime sur l'horaire
 * paroissial. Le libellé de l'horaire n'est qu'un complément d'affichage.
 */
export function formatCelebrationTime(demande) {
  if (!demande) return '—';

  const custom = formatTime(demande.heurePersonnalisee);
  if (custom) return custom;

  const scheduled = formatTime(demande.horaireHeure);
  if (scheduled) {
    return demande.horaireLibelle ? `${scheduled} · ${demande.horaireLibelle}` : scheduled;
  }

  return demande.horaireLibelle || '—';
}
