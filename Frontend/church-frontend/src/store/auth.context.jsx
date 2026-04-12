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
    };
  }
  return {
    isAuthenticated: false,
    user: null,
    token: null,
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(initialSession);

  const login = async (payload) => {
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
    });
    return { user };
  };

  const logout = () => {
    setActiveParishId(null);
    authService.logout();
    setSession({
      isAuthenticated: false,
      user: null,
      token: null,
    });
  };

  const value = useMemo(
    () => ({
      ...session,
      login,
      logout,
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