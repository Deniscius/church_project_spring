function parseDate(value) {
  if (!value) return null;
  const raw = String(value).trim();
  const isoDay = raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (isoDay) {
    const date = new Date(`${isoDay[1]}-${isoDay[2]}-${isoDay[3]}T12:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Affichage long : « 16 août 2026 ». */
export function formatDate(value) {
  if (!value) return '—';
  const date = parseDate(value);
  if (!date) return typeof value === 'string' ? value : '—';
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(date);
}

/** Date ISO (YYYY-MM-DD) → « 16/08/2026 ». */
export function formatDateShort(value) {
  if (!value) return '';
  const raw = String(value).slice(0, 10);
  const [y, m, d] = raw.split('-');
  if (!y || !m || !d || y.length !== 4) return raw;
  return `${d}/${m}/${y}`;
}
