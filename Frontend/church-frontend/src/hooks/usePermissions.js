import { useMemo } from 'react';
import { useAuth } from './useAuth';
import { isAdmin, isSuperAdmin, PERMISSIONS, ROLES } from '../constants/roles';

/**
 * Permissions effectives de la session.
 *
 * Le frontend n'invente aucun droit à partir du rôle : il consomme uniquement
 * la liste `user.permissions` fournie par le backend. Les guards UI améliorent
 * l'expérience mais ne remplacent jamais les contrôles Spring Security.
 */
export function usePermissions() {
  const { user } = useAuth();
  const userRole = user?.role;

  const allPermissions = useMemo(() => {
    if (!Array.isArray(user?.permissions)) return [];
    return [...new Set(user.permissions.filter(Boolean))];
  }, [user?.permissions]);

  const permissionSet = useMemo(() => new Set(allPermissions), [allPermissions]);
  const has = (permission) => Boolean(permission) && permissionSet.has(permission);

  return {
    has,
    allPermissions,

    // Compatibilité avec les anciens composants génériques de demande.
    can: {
      read: has(PERMISSIONS.DEMAND_READ),
      create: has(PERMISSIONS.DEMAND_EDIT),
      edit: has(PERMISSIONS.DEMAND_EDIT),
      delete: has(PERMISSIONS.DEMAND_DELETE),
      validate: has(PERMISSIONS.DEMAND_VALIDATE),
    },

    role: userRole,
    isAdminUser: isAdmin(userRole),
    isSuperAdminUser: isSuperAdmin(userRole),
    isSecretaire: userRole === ROLES.SECRETAIRE,
    isCure: userRole === ROLES.CURE,

    hasAnyRole: (...roles) => roles.includes(userRole),
    hasAllRoles: (...roles) => roles.every((role) => userRole === role),

    check: (condition) => {
      if (typeof condition === 'function') {
        return condition({ role: userRole, permissions: allPermissions });
      }
      return false;
    },

    // Compatibilité ciblée : privilégier `has(PERMISSIONS.X)` dans le nouveau code.
    hasRole: (roles = []) => roles.includes(userRole),
    canManageSettings: has(PERMISSIONS.PARISH_SETTINGS_MANAGE),
    canReadOnly: userRole === ROLES.CURE,
  };
}
