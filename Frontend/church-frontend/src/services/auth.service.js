import {
  AUTH_PAROISSES_KEY,
  AUTH_SELECTED_PAROISSE_KEY,
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
  authStorage,
  clearAuthStorage,
  migrateAuthStorage,
} from '../constants/authStorage';
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
    clearAuthStorage();
    return true;
  },

  getPersistedSession: () => {
    try {
      migrateAuthStorage();
      const token = authStorage.getItem(AUTH_TOKEN_KEY);
      const raw = authStorage.getItem(AUTH_USER_KEY);
      if (!token || !raw) return null;
      return { token, user: JSON.parse(raw) };
    } catch {
      return null;
    }
  },

  persistSession: (token, user, paroisses = [], selectedParoisse = null) => {
    authStorage.setItem(AUTH_TOKEN_KEY, token);
    authStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    authStorage.setItem(AUTH_PAROISSES_KEY, JSON.stringify(paroisses));
    if (selectedParoisse) {
      authStorage.setItem(AUTH_SELECTED_PAROISSE_KEY, JSON.stringify(selectedParoisse));
    } else {
      authStorage.removeItem(AUTH_SELECTED_PAROISSE_KEY);
    }
    // Drop any leftover tab-scoped session keys.
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_USER_KEY);
    sessionStorage.removeItem(AUTH_PAROISSES_KEY);
    sessionStorage.removeItem(AUTH_SELECTED_PAROISSE_KEY);
  },

  /**
   * Récupère les paroisses de l'utilisateur depuis la session
   */
  getSessionParoisses: () => {
    try {
      migrateAuthStorage();
      const raw = authStorage.getItem(AUTH_PAROISSES_KEY);
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
      migrateAuthStorage();
      const raw = authStorage.getItem(AUTH_SELECTED_PAROISSE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  /**
   * Définit la paroisse active (tenant)
   */
  setSelectedParoisse: (paroisse) => {
    if (paroisse) {
      authStorage.setItem(AUTH_SELECTED_PAROISSE_KEY, JSON.stringify(paroisse));
    } else {
      authStorage.removeItem(AUTH_SELECTED_PAROISSE_KEY);
    }
  },
};
