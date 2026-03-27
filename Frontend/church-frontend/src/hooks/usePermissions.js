import { useAuth } from './useAuth';

export function usePermissions() {
  const { user } = useAuth();

  const hasRole = (roles = []) => roles.includes(user?.role);

  return {
    role: user?.role,
    hasRole,
    canManageSettings: hasRole(['ADMIN', 'GESTIONNAIRE', 'SUPER_ADMIN']),
    canReadOnly: hasRole(['CONSULTATION']),
  };
}
