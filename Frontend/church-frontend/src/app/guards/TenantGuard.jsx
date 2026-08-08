import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';

export default function TenantGuard() {
  const { isAuthenticated, user } = useAuth();
  const { activeParish, loading, error } = useTenant();

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />;
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

  // Équipe plateforme : charger d'abord le catalogue modèle (contexte de travail).
  if (user?.isGlobal) {
    return <Navigate to="/admin/catalogue-modele" replace />;
  }

  return <Navigate to="/admin/login" replace />;
}
