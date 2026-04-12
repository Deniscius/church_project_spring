export function getBadgeClass(status) {
  if (!status) return 'badge';
  const normalized = String(status).toUpperCase();
  if (['VALIDEE', 'PAYE', 'TRAITEE', 'ACTIVE'].includes(normalized)) return 'badge success';
  if (['REJETEE', 'ECHEC', 'ECHOUE', 'INACTIVE'].includes(normalized)) return 'badge danger';
  if (['NON_PAYE', 'EN_ATTENTE'].includes(normalized)) return 'badge warning';
  return 'badge warning';
}
