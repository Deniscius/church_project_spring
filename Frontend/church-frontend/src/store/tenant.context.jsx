/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { getActiveParishId, setActiveParishId } from '../constants/authStorage';
import { useAuth } from '../hooks/useAuth';
import { comptabiliteService } from '../services/inscription.service';
import { parishService } from '../services/parish.service';
import { mapParoisseToTenant } from '../utils/apiMappers';

const TenantContext = createContext(null);

function sortTenants(list) {
  return [...list].sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'fr'));
}

export function TenantProvider({ children }) {
  const {
    user,
    isAuthenticated,
    paroisses,
    selectedParoisse,
    setSelectedParoisse,
  } = useAuth();
  const [activeParish, setActiveParishState] = useState(null);
  const [parishOptions, setParishOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  // Empêche TenantGuard de rediriger vers login au F5 avant la fin du bootstrap paroisse.
  const [bootstrappedUserId, setBootstrappedUserId] = useState(null);

  const selectedParishId = selectedParoisse?.publicId || selectedParoisse?.id || null;
  const currentUserId = user?.id ?? null;
  const isSuperAdmin = user?.isGlobal === true && user?.role === 'SUPER_ADMIN';
  /** Super Admin + Comptable plateforme : intervention / audit multi-tenant. */
  const isPlatformStaff = user?.isGlobal === true
    && (user?.role === 'SUPER_ADMIN' || user?.role === 'COMPTABLE');
  const awaitingBootstrap = Boolean(
    isAuthenticated && currentUserId && bootstrappedUserId !== currentUserId
  );

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      if (!isAuthenticated || !currentUserId) {
        setActiveParishState(null);
        setParishOptions([]);
        setError(null);
        setLoading(false);
        setBootstrappedUserId(null);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        let mapped = (paroisses || [])
          .filter((p) => p.active !== false && p.isActive !== false)
          .map(mapParoisseToTenant)
          .filter((p) => Boolean(p?.id));

        // Équipe plateforme : catalogue complet des tenants (hors modèle système).
        if (isPlatformStaff) {
          try {
            const all = await parishService.getAll();
            mapped = sortTenants(
              (Array.isArray(all) ? all : [])
                .filter((p) => !p?.isSystem)
                .map(mapParoisseToTenant)
                .filter((p) => Boolean(p?.id))
            );
          } catch (e) {
            if (!cancelled) {
              setError(e instanceof Error ? e.message : 'Impossible de charger les paroisses');
            }
          }
        }

        if (cancelled) return;

        const savedId = getActiveParishId();
        let pick = mapped.find((p) => p.id === selectedParishId)
          || mapped.find((p) => p.id === savedId)
          || null;

        // Locaux : une seule paroisse → sélection auto.
        if (!pick && !isPlatformStaff && mapped.length === 1) {
          pick = mapped[0];
        }

        // Équipe plateforme (comptable) : paroisse modèle sans rattachement.
        if (!pick && user?.isGlobal && !isSuperAdmin && (selectedParishId || savedId)) {
          try {
            const template = await comptabiliteService.getCatalogueModele();
            pick = mapParoisseToTenant(template);
          } catch {
            pick = null;
          }
        }

        // Plateforme : conserver le modèle catalogue si c’était le contexte sauvegardé.
        if (!pick && isPlatformStaff && savedId) {
          const fromList = mapped.find((p) => p.id === savedId);
          if (fromList) {
            pick = fromList;
          } else {
            try {
              const template = await comptabiliteService.getCatalogueModele();
              const mappedTemplate = mapParoisseToTenant(template);
              if (mappedTemplate?.id === savedId) {
                pick = mappedTemplate;
              }
            } catch {
              pick = null;
            }
          }
        }

        if (cancelled) return;

        setParishOptions(mapped);
        setActiveParishState((prev) => (prev?.id === pick?.id ? prev : pick));
        setActiveParishId(pick?.id || null);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Paroisse indisponible');
      } finally {
        if (!cancelled) {
          setLoading(false);
          setBootstrappedUserId(currentUserId);
        }
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, currentUserId, user?.isGlobal, isSuperAdmin, isPlatformStaff, paroisses, selectedParishId]);

  const setActiveParish = useCallback((tenant) => {
    const nextId = tenant?.id || null;
    const raw = tenant?.raw || null;
    const previousId = getActiveParishId();

    setActiveParishState(tenant || null);
    setActiveParishId(nextId);

    if (nextId && nextId === previousId) {
      return;
    }
    setSelectedParoisse(raw);
  }, [setSelectedParoisse]);

  const intervening = Boolean(
    isPlatformStaff && activeParish?.id && !activeParish?.isSystem
  );

  const value = useMemo(
    () => ({
      activeParish,
      parishOptions,
      setActiveParish,
      loading: loading || awaitingBootstrap,
      error,
      /**
       * Équipe plateforme en contrôle / audit d’un tenant réel
       * (pas le catalogue modèle).
       */
      isIntervening: intervening,
      /** @deprecated préférer isIntervening */
      isOperatingParish: intervening,
    }),
    [activeParish, parishOptions, setActiveParish, loading, awaitingBootstrap, error, intervening]
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
