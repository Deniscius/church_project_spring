import { createContext, useContext, useMemo, useState } from 'react';
import { setActiveParishId } from '../constants/authStorage';
import { authService } from '../services/auth.service';
import { mapJwtToUser } from '../utils/mapJwtToUser';

const AuthContext = createContext(null);

function initialSession() {
  const persisted = authService.getPersistedSession();
  if (persisted) {
    return {
      isAuthenticated: true,
      user: persisted.user,
      token: persisted.token,
      paroisses: authService.getSessionParoisses(),
      selectedParoisse: authService.getSelectedParoisse(),
    };
  }
  return {
    isAuthenticated: false,
    user: null,
    token: null,
    paroisses: [],
    selectedParoisse: null,
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(initialSession);

  /**
   * Login multi-tenant (nouveau - recommandé)
   * Retourne : { token, user, paroisses, selectedParoisse }
   */
  const loginMultiTenant = async (payload) => {
    try {
      const response = await authService.loginMultiTenant({
        username: payload.username,
        password: payload.password,
      });
      
      const { token, user, paroisses = [], selectedParoisse } = response;
      
      authService.persistSession(token, user, paroisses, selectedParoisse);
      
      setSession({
        isAuthenticated: true,
        user,
        token,
        paroisses,
        selectedParoisse,
      });
      
      return { user, paroisses, selectedParoisse };
    } catch (error) {
      console.error('Multi-tenant login failed:', error);
      throw error;
    }
  };

  /**
   * Login classique (legacy - pour compatibilité)
   */
  const login = async (payload) => {
    try {
      const jwt = await authService.login({
        username: payload.username,
        password: payload.password,
      });
      const user = mapJwtToUser(jwt);
      authService.persistSession(jwt.accessToken, user);
      setSession({
        isAuthenticated: true,
        user,
        token: jwt.accessToken,
        paroisses: [],
        selectedParoisse: null,
      });
      return { user };
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const logout = () => {
    setActiveParishId(null);
    authService.logout();
    setSession({
      isAuthenticated: false,
      user: null,
      token: null,
      paroisses: [],
      selectedParoisse: null,
    });
  };

  /**
   * Change la paroisse active (tenant)
   */
  const setSelectedParoisse = (paroisse) => {
    authService.setSelectedParoisse(paroisse);
    setSession((prev) => ({
      ...prev,
      selectedParoisse: paroisse,
    }));
  };

  const value = useMemo(
    () => ({
      ...session,
      login,
      loginMultiTenant,
      logout,
      setSelectedParoisse,
    }),
    [session]
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