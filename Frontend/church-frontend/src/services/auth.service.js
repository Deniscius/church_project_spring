import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/authStorage';
import { apiClient } from './http/apiClient';

const MULTI_TENANT_ENDPOINT = '/auth/login-multi-tenant';
const LEGACY_ENDPOINT = '/auth/login';

export const authService = {
  /**
   * Login avec support multi-tenant
   * Retourne : { token, user, paroisses, selectedParoisse }
   */
  loginMultiTenant: async ({ username, password }) => {
    try {
      const response = await apiClient(MULTI_TENANT_ENDPOINT, {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      });
      return response;
    } catch (err) {
      console.error('Multi-tenant login failed:', err);
      throw err;
    }
  },

  /**
   * Login classique (deprecated - utilise loginMultiTenant)
   */
  login: async ({ username, password }) =>
    apiClient(LEGACY_ENDPOINT, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  logout: async () => {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    sessionStorage.removeItem('selectedParoisse');
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

  persistSession: (token, user, paroisses = [], selectedParoisse = null) => {
    sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    sessionStorage.setItem('paroisses', JSON.stringify(paroisses));
    if (selectedParoisse) {
      sessionStorage.setItem('selectedParoisse', JSON.stringify(selectedParoisse));
    }
  },

  /**
   * Récupère les paroisses de l'utilisateur depuis la session
   */
  getSessionParoisses: () => {
    try {
      const raw = sessionStorage.getItem('paroisses');
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  },

  /**
   * Récupère la paroisse sélectionnée
   */
  getSelectedParoisse: () => {
    try {
      const raw = sessionStorage.getItem('selectedParoisse');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  /**
   * Définit la paroisse active (tenant)
   */
  setSelectedParoisse: (paroisse) => {
    sessionStorage.setItem('selectedParoisse', JSON.stringify(paroisse));
  },
};
