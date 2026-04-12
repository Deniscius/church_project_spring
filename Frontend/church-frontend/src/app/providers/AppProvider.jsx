import React from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '../../lib/queryClient';
import { AuthProvider } from '../../store/auth.context';
import { TenantProvider } from '../../store/tenant.context';
import { UIProvider } from '../../store/ui.context';

export function AppProvider({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TenantProvider>
          <UIProvider>{children}</UIProvider>
        </TenantProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}