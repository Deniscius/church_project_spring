import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useTenant } from '../../hooks/useTenant';

export default function TenantGuard() {
  const { activeParish } = useTenant();
  return activeParish ? <Outlet /> : <Navigate to="/admin/login" replace />;
}
