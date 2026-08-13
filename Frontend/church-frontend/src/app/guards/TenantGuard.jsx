import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { ROUTES } from '../../constants/routes';

export default function TenantGuard() {
  const { isAuthenticated, user } = useAuth();
  const { activeParish, loading, error } = useTenant();

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (loading) {
    return (
      <div className="page-section">
        <p className="muted">Chargement de la paroisse…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-section">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  if (activeParish) {
    return <Outlet />;
  }

  // Super Admin : choisir un tenant depuis l’annuaire avant les écrans paroissiaux.
  if (user?.isGlobal && user?.role === 'SUPER_ADMIN') {
    return <Navigate to={ROUTES.PARISHES} replace />;
  }

  // Comptable plateforme : audit global (pas besoin d’un tenant pour démarrer).
  if (user?.isGlobal && user?.role === 'COMPTABLE') {
    return <Navigate to={ROUTES.PLATFORM_DEMANDES} replace />;
  }

  // Autre compte global : catalogue modèle.
  if (user?.isGlobal) {
    return <Navigate to={ROUTES.CATALOGUE_MODELE} replace />;
  }

  return <Navigate to={ROUTES.LOGIN} replace />;
}
