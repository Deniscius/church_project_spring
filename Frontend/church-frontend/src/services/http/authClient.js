import { apiClient } from './apiClient';

export function authClient(path, options = {}) {
  return apiClient(path, options, { auth: true });
}
