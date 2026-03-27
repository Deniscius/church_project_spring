import { createContext, useContext, useMemo, useState } from 'react';
import { mockParish } from '../data/mockData';

const TenantContext = createContext(null);

export function TenantProvider({ children }) {
  const [activeParish, setActiveParish] = useState(mockParish);

  const value = useMemo(
    () => ({
      activeParish,
      setActiveParish,
    }),
    [activeParish]
  );

  return <TenantContext.Provider value={value}>{children}</TenantContext.Provider>;
}

export function useTenantStore() {
  const context = useContext(TenantContext);

  if (!context) {
    throw new Error('useTenantStore must be used inside TenantProvider');
  }

  return context;
}