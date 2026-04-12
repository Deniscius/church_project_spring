import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/authStorage';
import { apiClient } from './http/apiClient';

export const authService = {
  login: async ({ username, password }) =>
    apiClient('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  logout: async () => {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    return true;
  },

  getPersistedSession: () => {
    try {
      const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
      const raw = sessionStorage.getItem(AUTH_USER_KEY);
      if (!token || !raw) return null;
      return { token, user: JSON.parse(raw) };
    } catch {
      return null;
    }
  },

  persistSession: (token, user) => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  },
};
