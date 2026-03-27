import { useTenantStore } from '../store/tenant.context';

export function useTenant() {
  return useTenantStore();
}
