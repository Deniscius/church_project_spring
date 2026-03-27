import { useAuthStore } from '../store/auth.context';

export function useAuth() {
  return useAuthStore();
}