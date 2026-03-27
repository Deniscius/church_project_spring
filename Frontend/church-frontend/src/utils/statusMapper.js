export function getBadgeClass(status) {
  if (!status) return 'badge';
  const normalized = String(status).toUpperCase();
  if (['VALIDEE', 'PAYE', 'TRAITEE', 'ACTIVE'].includes(normalized)) return 'badge success';
  if (['REJETEE', 'ECHEC', 'INACTIVE'].includes(normalized)) return 'badge danger';
  return 'badge warning';
}
