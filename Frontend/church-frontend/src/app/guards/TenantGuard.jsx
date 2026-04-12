import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';

export default function TenantGuard() {
  const { isAuthenticated } = useAuth();
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

  return activeParish ? <Outlet /> : <Navigate to="/admin/login" replace />;
}
