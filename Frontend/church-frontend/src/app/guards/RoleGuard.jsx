import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function RoleGuard({ allowedRoles = [] }) {
  const { user } = useAuth();

  if (!allowedRoles.length || allowedRoles.includes(user?.role)) {
    return <Outlet />;
  }

  return <Navigate to="/admin/dashboard" replace />;
}
