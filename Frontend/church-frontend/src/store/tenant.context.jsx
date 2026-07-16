/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getActiveParishId, setActiveParishId } from '../constants/authStorage';
import { useAuth } from '../hooks/useAuth';
import { mapParoisseToTenant } from '../utils/apiMappers';

const TenantContext = createContext(null);

export function TenantProvider({ children }) {
  const {
    user,
    token,
    isAuthenticated,
    paroisses,
    selectedParoisse,
    setSelectedParoisse,
  } = useAuth();
  const [activeParish, setActiveParishState] = useState(null);
  const [parishOptions, setParishOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      if (!isAuthenticated || !token || !user?.id) {
        setActiveParishState(null);
        setParishOptions([]);
        setError(null);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        // La réponse de connexion est la source de vérité : elle ne contient
        // que les paroisses auxquelles cet utilisateur a réellement accès.
        const mapped = (paroisses || [])
          .filter((p) => p.active !== false && p.isActive !== false)
          .map(mapParoisseToTenant)
          .filter((p) => Boolean(p?.id));

        if (cancelled) return;

        const selectedId = selectedParoisse?.id || selectedParoisse?.publicId;
        const savedId = getActiveParishId();
        const pick = mapped.find((p) => p.id === selectedId)
          || mapped.find((p) => p.id === savedId)
          || mapped[0]
          || null;

        setParishOptions(mapped);
        setActiveParishState(pick);
        setActiveParishId(pick?.id || null);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Paroisse indisponible');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, token, user?.id, paroisses, selectedParoisse]);

  const value = useMemo(
    () => ({
      activeParish,
      parishOptions,
      setActiveParish: (tenant) => {
        setActiveParishState(tenant);
        setActiveParishId(tenant?.id || null);
        setSelectedParoisse(tenant?.raw || null);
      },
      loading,
      error,
    }),
    [activeParish, parishOptions, loading, error, setSelectedParoisse]
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
