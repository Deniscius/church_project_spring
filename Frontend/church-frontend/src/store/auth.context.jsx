import { createContext, useContext, useMemo, useState } from 'react';
import { mockParish, mockUser } from '../data/mockData';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState({
    isAuthenticated: true,
    user: mockUser,
    token: 'demo-token',
  });

  const login = async (payload) => {
    const updatedUser = {
      ...mockUser,
      username: payload?.username || mockUser.username,
    };

    setSession({
      isAuthenticated: true,
      user: updatedUser,
      token: 'demo-token',
    });

    return { user: updatedUser, parish: mockParish };
  };

  const logout = () => {
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