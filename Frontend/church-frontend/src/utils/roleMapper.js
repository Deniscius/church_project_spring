import { ROLE_LABELS } from '../constants/roles';

export function formatRole(role) {
  return ROLE_LABELS[role] || role || '—';
}
