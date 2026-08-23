import { apiClient } from './http/apiClient';

export const administrativeAuditService = {
  listRecent: () => apiClient('/administrative-audit', {}, { auth: true }),
};
