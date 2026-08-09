import React from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '../../lib/queryClient';
import { ToastProvider } from '../../contexts/toast.context';
import { AuthProvider } from '../../store/auth.context';
import { TenantProvider } from '../../store/tenant.context';
import { UIProvider } from '../../store/ui.context';
import { ThemeProvider } from '../../store/theme.context';

export function AppProvider({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <TenantProvider>
              <UIProvider>{children}</UIProvider>
            </TenantProvider>
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
