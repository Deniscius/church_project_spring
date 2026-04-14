import { useAuth } from './useAuth';
import {
  hasPermission,
  canRead,
  canCreate,
  canEdit,
  canDelete,
  canValidate,
  isAdmin,
  isSuperAdmin,
  isHierarchyGreaterOrEqual,
  ROLE_PERMISSIONS,
  ROLES,
} from '../constants/roles';

/**
 * Hook pour vérifier les permissions et les rôles de l'utilisateur.
 * Facilite les vérifications partout dans l'application.
 * 
 * Usage:
 * const { can, has, isAdminUser, isSuperAdminUser } = usePermissions();
 * if (can.edit) { ... }
 */
export function usePermissions() {
  const { user } = useAuth();
  const userRole = user?.role;

  return {
    // Fonctions de permission granulaires
    has: (permission) => hasPermission(userRole, permission),
    can: {
      read: canRead(userRole),
      create: canCreate(userRole),
      edit: canEdit(userRole),
      delete: canDelete(userRole),
      validate: canValidate(userRole),
    },

    // Vérifications de rôle simple
    role: userRole,
    isAdminUser: isAdmin(userRole),
    isSuperAdminUser: isSuperAdmin(userRole),
    isSecretaire: userRole === ROLES.SECRETAIRE,
    isCure: userRole === ROLES.CURE,

    // Hiérarchie
    isHierarchyGreaterOrEqual: (requiredRole) =>
      isHierarchyGreaterOrEqual(userRole, requiredRole),

    // Vérification multi-rôles
    hasAnyRole: (...roles) => roles.includes(userRole),
    hasAllRoles: (...roles) => roles.every((r) => userRole === r),

    // Retourner les permissions du rôle
    allPermissions: ROLE_PERMISSIONS[userRole] || [],

    // Vérification personnalisée
    check: (condition) => {
      if (typeof condition === 'function') {
        return condition(userRole);
      }
      return false;
    },

    // Pour compatibilité avec l'ancienne API
    hasRole: (roles = []) => roles.includes(userRole),
    canManageSettings: isAdmin(userRole),
    canReadOnly: userRole === ROLES.CURE,
  };
}
