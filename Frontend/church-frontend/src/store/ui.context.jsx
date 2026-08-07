/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const UIContext = createContext(null);

export function UIProvider({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    document.body.classList.toggle('sidebar-open', sidebarOpen);
    return () => document.body.classList.remove('sidebar-open');
  }, [sidebarOpen]);

  const value = useMemo(
    () => ({
      sidebarOpen,
      setSidebarOpen,
    }),
    [sidebarOpen]
  );

  return <UIContext.Provider value={value}>{children}</UIContext.Provider>;
}

export function useUIStore() {
  const context = useContext(UIContext);

  if (!context) {
    throw new Error('useUIStore must be used inside UIProvider');
  }

  return context;
}
