import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';

/**
 * RoleGuard - Protège les routes basées sur les rôles.
 * 
 * Usage:
 * <Route element={<RoleGuard allowedRoles={['ADMIN', 'SUPER_ADMIN']} />}>
 *   <Route path="/admin/dashboard" element={<AdminDashboard />} />
 * </Route>
 * 
 * Props:
 * - allowedRoles: array de rôles autorisés
 * - fallbackPath: chemin de redirection (défaut: /unauthorized)
 * - requireAll: booléen - si true, l'utilisateur doit avoir TOUS les rôles
 */
export default function RoleGuard({ 
  allowedRoles = [], 
  fallbackPath = '/unauthorized',
  requireAll = false 
}) {
  const { user, isAuthenticated } = useAuth();
  const { hasAnyRole, hasAllRoles } = usePermissions();

  // ✅ Si pas d'authentification, rediriger vers login
  if (!isAuthenticated || !user) {
    return <Navigate to="/admin/login" replace />;
  }

  // ✅ Si pas de restrictions ou l'utilisateur a les bons rôles
  if (!allowedRoles.length) {
    return <Outlet />;
  }

  const hasAccess = requireAll 
    ? hasAllRoles(...allowedRoles) 
    : hasAnyRole(...allowedRoles);

  if (hasAccess) {
    return <Outlet />;
  }

  // ❌ Accès refusé
  console.warn(
    `Access denied for role ${user.role}. Required roles: ${allowedRoles.join(', ')}`
  );
  return <Navigate to={fallbackPath} replace />;
}
