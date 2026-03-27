import React from 'react';
import { AuthProvider } from '../../store/auth.context';
import { TenantProvider } from '../../store/tenant.context';
import { UIProvider } from '../../store/ui.context';

export function AppProvider({ children }) {
  return (
    <AuthProvider>
      <TenantProvider>
        <UIProvider>{children}</UIProvider>
      </TenantProvider>
    </AuthProvider>
  );
}