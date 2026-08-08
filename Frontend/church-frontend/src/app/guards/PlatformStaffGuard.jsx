import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/** Accès équipe plateforme globale : SUPER_ADMIN ou COMPTABLE. */
export default function PlatformStaffGuard() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/admin/login" replace />;
  }

  const allowed =
    user.isGlobal === true
    && (user.role === 'SUPER_ADMIN' || user.role === 'COMPTABLE');

  return allowed ? <Outlet /> : <Navigate to="/unauthorized" replace />;
}
