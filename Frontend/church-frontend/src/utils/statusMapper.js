/** Libellés métier des statuts de paiement, tels qu'attendus au secrétariat. */
export const PAYMENT_STATUS_LABELS = {
  NON_PAYE: 'Non payée',
  EN_ATTENTE: 'Paiement en cours',
  PAYE: 'Payée',
  ECHOUE: 'Échouée',
};

export function paymentStatusLabel(status) {
  return PAYMENT_STATUS_LABELS[status] || status || '—';
}

/**
 * Cycle de vie commercial d'une paroisse, aligné sur l'enum StatutTenant du
 * backend. « Annuaire » désigne les paroisses du diocèse importées sans
 * démarche commerciale : elles ne sont pas en attente, elles sont à démarcher.
 */
export const TENANT_STATUS_LABELS = {
  PROSPECT: 'Annuaire',
  EN_ATTENTE_PAIEMENT: 'En attente de paiement',
  ACTIVE: 'Abonnée',
  EN_TOLERANCE: 'Échéance dépassée',
  SUSPENDUE: 'Suspendue',
  RESILIEE: 'Résiliée',
};

export function tenantStatusLabel(statut) {
  return TENANT_STATUS_LABELS[statut] || statut || '—';
}

export function getBadgeClass(status) {
  if (!status) return 'badge';
  const normalized = String(status).toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
  if (normalized === 'PROSPECT') return 'badge';
  if (['VALIDEE', 'PAYE', 'TRAITEE', 'TERMINEE', 'ACTIVE', 'APPROUVEE'].includes(normalized)) return 'badge success';
  if (
    ['REJETEE', 'ECHEC', 'ECHOUE', 'INACTIVE', 'EXPIRED', 'ABONNEMENT EXPIRE',
      'SUSPENDUE', 'RESILIEE'].includes(normalized)
  ) {
    return 'badge danger';
  }
  if (
    ['NON_PAYE', 'EN_ATTENTE', 'PENDING_SUB', 'SOUMISE', 'EN ATTENTE ABO.'].includes(normalized)
    || normalized.includes('ATTENTE')
  ) {
    return 'badge warning';
  }
  return 'badge warning';
}

