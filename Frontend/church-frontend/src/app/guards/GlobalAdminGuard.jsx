import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

/** Uniquement SUPER_ADMIN global (config système). */
export default function GlobalAdminGuard() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/admin/login" replace />;
  }

  const isGlobalSuperAdmin = user.role === 'SUPER_ADMIN' && user.isGlobal === true;
  return isGlobalSuperAdmin ? <Outlet /> : <Navigate to="/unauthorized" replace />;
}
