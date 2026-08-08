import {
  AUTH_PAROISSES_KEY,
  AUTH_SELECTED_PAROISSE_KEY,
  AUTH_USER_KEY,
  authStorage,
  clearAuthStorage,
  hasSessionFlag,
  migrateAuthStorage,
  setSessionFlag,
} from '../constants/authStorage';
import { apiClient } from './http/apiClient';

const MULTI_TENANT_ENDPOINT = '/auth/login-multi-tenant';
const LEGACY_ENDPOINT = '/auth/login';

function mapLoginUser(user) {
  if (!user) return null;
  return {
    id: user.id || user.publicId,
    nom: user.nom || '',
    prenom: user.prenom || '',
    username: user.username,
    role: user.role,
    isGlobal: Boolean(user.isGlobal),
  };
}

function mapParoisses(list = []) {
  return (list || []).map((p) => ({
    id: p.id || p.paroisseId || p.publicId,
    publicId: p.id || p.paroisseId || p.publicId,
    nom: p.nom || p.paroisseNom || '',
    name: p.nom || p.paroisseNom || '',
    adresse: p.adresse || '',
    roleParoisse: p.roleParoisse,
    active: p.active !== false,
    subscriptionExpiresAt: p.subscriptionExpiresAt || null,
    raw: p,
  }));
}

export const authService = {
  /**
   * Login multi-tenant — le JWT est posé en cookie HttpOnly par le serveur.
   */
  loginMultiTenant: async ({ username, password }) => {
    const response = await apiClient(MULTI_TENANT_ENDPOINT, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    return {
      user: mapLoginUser(response?.user),
      paroisses: mapParoisses(response?.paroisses),
      selectedParoisse: response?.selectedParoisse
        ? mapParoisses([response.selectedParoisse])[0]
        : null,
    };
  },

  login: async ({ username, password }) =>
    apiClient(LEGACY_ENDPOINT, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  /** Restaure la session depuis le cookie (sans JWT côté client). */
  fetchCurrentSession: async () => {
    const response = await apiClient('/auth/me', {}, { auth: true });
    return {
      user: mapLoginUser(response?.user),
      paroisses: mapParoisses(response?.paroisses),
      selectedParoisse: response?.selectedParoisse
        ? mapParoisses([response.selectedParoisse])[0]
        : null,
    };
  },

  forgotPassword: async (usernameOrEmail) =>
    apiClient('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ usernameOrEmail }),
    }),

  resetPassword: async ({ token, newPassword }) =>
    apiClient('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword }),
    }),

  logout: async () => {
    try {
      await apiClient('/auth/logout', { method: 'POST' }, { auth: true });
    } catch {
      // Cookie déjà expiré : on nettoie quand même le stockage profil.
    }
    clearAuthStorage();
    return true;
  },

  getPersistedSession: () => {
    try {
      migrateAuthStorage();
      if (!hasSessionFlag()) return null;
      const raw = authStorage.getItem(AUTH_USER_KEY);
      if (!raw) return null;
      return {
        user: JSON.parse(raw),
        // Sentinel : auth réelle = cookie serveur, pas un JWT lisible.
        token: 'cookie',
      };
    } catch {
      return null;
    }
  },

  persistSession: (user, paroisses = [], selectedParoisse = null) => {
    setSessionFlag(true);
    authStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    authStorage.setItem(AUTH_PAROISSES_KEY, JSON.stringify(paroisses));
    if (selectedParoisse) {
      authStorage.setItem(AUTH_SELECTED_PAROISSE_KEY, JSON.stringify(selectedParoisse));
    } else {
      authStorage.removeItem(AUTH_SELECTED_PAROISSE_KEY);
    }
    // Purge historique JWT.
    authStorage.removeItem('church_auth_token');
    sessionStorage.removeItem('church_auth_token');
  },

  getSessionParoisses: () => {
    try {
      migrateAuthStorage();
      const raw = authStorage.getItem(AUTH_PAROISSES_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  },

  getSelectedParoisse: () => {
    try {
      migrateAuthStorage();
      const raw = authStorage.getItem(AUTH_SELECTED_PAROISSE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  setSelectedParoisse: (paroisse) => {
    if (paroisse) {
      authStorage.setItem(AUTH_SELECTED_PAROISSE_KEY, JSON.stringify(paroisse));
    } else {
      authStorage.removeItem(AUTH_SELECTED_PAROISSE_KEY);
    }
  },
};
