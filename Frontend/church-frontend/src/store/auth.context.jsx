/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { clearAuthStorage, setActiveParishId } from '../constants/authStorage';
import { authService } from '../services/auth.service';
import { mapJwtToUser } from '../utils/mapJwtToUser';

const AuthContext = createContext(null);

function initialSession() {
  const persisted = authService.getPersistedSession();
  if (persisted) {
    return {
      isAuthenticated: true,
      authReady: false,
      user: persisted.user,
      token: 'cookie',
      paroisses: authService.getSessionParoisses(),
      selectedParoisse: authService.getSelectedParoisse(),
      sessionExpired: false,
    };
  }
  return {
    isAuthenticated: false,
    authReady: true,
    user: null,
    token: null,
    paroisses: [],
    selectedParoisse: null,
    sessionExpired: false,
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(initialSession);

  useEffect(() => {
    const handleSessionExpired = () => {
      clearAuthStorage();
      setSession({
        isAuthenticated: false,
        authReady: true,
        user: null,
        token: null,
        paroisses: [],
        selectedParoisse: null,
        sessionExpired: true,
      });
    };
    window.addEventListener('church:session-expired', handleSessionExpired);
    return () => window.removeEventListener('church:session-expired', handleSessionExpired);
  }, []);

  // Valide le cookie HttpOnly au démarrage (purge si expiré / révoqué).
  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      const persisted = authService.getPersistedSession();
      if (!persisted) {
        if (!cancelled) {
          setSession((prev) => ({ ...prev, authReady: true }));
        }
        return;
      }
      try {
        const live = await authService.fetchCurrentSession();
        if (cancelled) return;
        authService.persistSession(live.user, live.paroisses, live.selectedParoisse);
        setSession({
          isAuthenticated: true,
          authReady: true,
          user: live.user,
          token: 'cookie',
          paroisses: live.paroisses,
          selectedParoisse: live.selectedParoisse,
          sessionExpired: false,
        });
      } catch {
        if (cancelled) return;
        clearAuthStorage();
        setSession({
          isAuthenticated: false,
          authReady: true,
          user: null,
          token: null,
          paroisses: [],
          selectedParoisse: null,
          sessionExpired: false,
        });
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  const loginMultiTenant = useCallback(async (payload) => {
    try {
      const response = await authService.loginMultiTenant({
        username: payload.username,
        password: payload.password,
      });

      const { user, paroisses = [], selectedParoisse } = response;
      authService.persistSession(user, paroisses, selectedParoisse);
      setSession({
        isAuthenticated: true,
        authReady: true,
        user,
        token: 'cookie',
        paroisses,
        selectedParoisse,
        sessionExpired: false,
      });
      return { user, paroisses, selectedParoisse };
    } catch (error) {
      console.error('Multi-tenant login failed:', error);
      throw error;
    }
  }, []);

  const login = useCallback(async (payload) => {
    try {
      const jwt = await authService.login({
        username: payload.username,
        password: payload.password,
      });
      const user = mapJwtToUser(jwt);
      authService.persistSession(user, [], null);
      setSession({
        isAuthenticated: true,
        authReady: true,
        user,
        token: 'cookie',
        paroisses: [],
        selectedParoisse: null,
        sessionExpired: false,
      });
      return { user };
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  }, []);

  const logout = useCallback(async () => {
    setActiveParishId(null);
    await authService.logout();
    setSession({
      isAuthenticated: false,
      authReady: true,
      user: null,
      token: null,
      paroisses: [],
      selectedParoisse: null,
      sessionExpired: false,
    });
  }, []);

  const patchCurrentUser = useCallback((patch) => {
    setSession((prev) => {
      if (!prev.user) return prev;
      const user = { ...prev.user, ...patch };
      authService.persistSession(user, prev.paroisses, prev.selectedParoisse);
      return { ...prev, user };
    });
  }, []);

  const setSelectedParoisse = useCallback((paroisse) => {
    authService.setSelectedParoisse(paroisse);
    setSession((prev) => {
      const prevId = prev.selectedParoisse?.publicId || prev.selectedParoisse?.id || null;
      const nextId = paroisse?.publicId || paroisse?.id || null;
      if (prevId === nextId) return prev;
      return {
        ...prev,
        selectedParoisse: paroisse,
      };
    });
  }, []);

  const value = useMemo(
    () => ({
      ...session,
      login,
      loginMultiTenant,
      logout,
      setSelectedParoisse,
      patchCurrentUser,
    }),
    [session, login, loginMultiTenant, logout, setSelectedParoisse, patchCurrentUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthStore() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuthStore must be used inside AuthProvider');
  }

  return context;
}
