import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getActiveParishId, setActiveParishId } from '../constants/authStorage';
import { useAuth } from '../hooks/useAuth';
import { parishAccessService } from '../services/parishAccess.service';
import { parishService } from '../services/parish.service';
import { mapParoisseToTenant } from '../utils/apiMappers';

const TenantContext = createContext(null);

export function TenantProvider({ children }) {
  const { user, token, isAuthenticated } = useAuth();
  const [activeParish, setActiveParishState] = useState(null);
  const [parishOptions, setParishOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const setActiveParish = (tenant) => {
    setActiveParishState(tenant);
    if (tenant?.id) setActiveParishId(tenant.id);
    else setActiveParishId(null);
  };

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
        if (user.role === 'ADMIN') {
          const list = await parishService.getAll();
          if (cancelled) return;
          const mapped = list
            .filter((p) => p.isActive !== false)
            .map(mapParoisseToTenant);
          setParishOptions(mapped);
          const saved = getActiveParishId();
          const pick = mapped.find((p) => p.id === saved) || mapped[0] || null;
          setActiveParishState(pick);
          if (pick && !saved) setActiveParishId(pick.id);
        } else {
          const accesses = await parishAccessService.getByUser(user.id);
          if (cancelled) return;
          const actives = (accesses || []).filter((a) => a.active);
          const mapped = actives.map((a) =>
            mapParoisseToTenant({
              publicId: a.paroissePublicId,
              nom: a.paroisseNom,
              localiteVille: '',
              email: '',
              telephone: '',
              isActive: true,
            })
          );
          setParishOptions(mapped);
          const saved = getActiveParishId();
          const pick = mapped.find((p) => p.id === saved) || mapped[0] || null;
          setActiveParishState(pick);
          if (pick && !saved) setActiveParishId(pick.id);
        }
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
  }, [isAuthenticated, token, user?.id, user?.role]);

  const value = useMemo(
    () => ({
      activeParish,
      parishOptions,
      setActiveParish,
      loading,
      error,
    }),
    [activeParish, parishOptions, loading, error]
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
