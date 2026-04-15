import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';

/**
 * PermissionGuard - Protège les routes basées sur les permissions granulaires.
 * 
 * Usage:
 * <Route element={<PermissionGuard requiredPermissions={['edit', 'delete']} />}>
 *   <Route path="/admin/edit" element={<EditPage />} />
 * </Route>
 * 
 * Props:
 * - requiredPermissions: array de permissions requises
 * - fallbackPath: chemin de redirection
 * - requireAll: booléen - si true, toutes les permissions sont requises
 */
export default function PermissionGuard({
  requiredPermissions = [],
  fallbackPath = '/unauthorized',
  requireAll = true,
}) {
  const { isAuthenticated } = useAuth();
  const { has, allPermissions } = usePermissions();

  // ✅ Si pas d'authentification
  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace />;
  }

  // ✅ Si pas de restrictions
  if (!requiredPermissions.length) {
    return <Outlet />;
  }

  // Vérifier les permissions
  const hasAllPermissions = requiredPermissions.every(perm => has(perm));
  const hasAnyPermission = requiredPermissions.some(perm => has(perm));

  const hasAccess = requireAll ? hasAllPermissions : hasAnyPermission;

  if (hasAccess) {
    return <Outlet />;
  }

  // ❌ Accès refusé
  console.warn(
    `Insufficient permissions. Required: ${requiredPermissions.join(', ')}. Has: ${allPermissions.join(', ')}`
  );
  return <Navigate to={fallbackPath} replace />;
}
