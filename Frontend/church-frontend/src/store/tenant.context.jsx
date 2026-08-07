/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { getActiveParishId, setActiveParishId } from '../constants/authStorage';
import { useAuth } from '../hooks/useAuth';
import { comptabiliteService } from '../services/inscription.service';
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

  const selectedParishId = selectedParoisse?.publicId || selectedParoisse?.id || null;

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
        const mapped = (paroisses || [])
          .filter((p) => p.active !== false && p.isActive !== false)
          .map(mapParoisseToTenant)
          .filter((p) => Boolean(p?.id));

        if (cancelled) return;

        const savedId = getActiveParishId();
        let pick = mapped.find((p) => p.id === selectedParishId)
          || mapped.find((p) => p.id === savedId)
          || mapped[0]
          || null;

        // Équipe plateforme : paroisse modèle sans rattachement à la connexion.
        if (!pick && user?.isGlobal && (selectedParishId || savedId)) {
          try {
            const template = await comptabiliteService.getCatalogueModele();
            pick = mapParoisseToTenant(template);
          } catch {
            pick = null;
          }
        }

        if (cancelled) return;

        setParishOptions(mapped);
        setActiveParishState((prev) => (prev?.id === pick?.id ? prev : pick));
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
  }, [isAuthenticated, token, user?.id, user?.isGlobal, paroisses, selectedParishId]);

  const setActiveParish = useCallback((tenant) => {
    const nextId = tenant?.id || null;
    const raw = tenant?.raw || null;
    const previousId = getActiveParishId();

    setActiveParishState(tenant);
    setActiveParishId(nextId);

    if (nextId && nextId === previousId) {
      return;
    }
    setSelectedParoisse(raw);
  }, [setSelectedParoisse]);

  const value = useMemo(
    () => ({
      activeParish,
      parishOptions,
      setActiveParish,
      loading,
      error,
    }),
    [activeParish, parishOptions, setActiveParish, loading, error]
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
