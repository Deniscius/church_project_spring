import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/**
 * Guard de périmètre plateforme.
 * Les capacités métier sont contrôlées séparément par PermissionGuard.
 */
export default function PlatformStaffGuard() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/admin/login" replace />;
  }

  return user.isGlobal === true
    ? <Outlet />
    : <Navigate to="/unauthorized" replace />;
}
