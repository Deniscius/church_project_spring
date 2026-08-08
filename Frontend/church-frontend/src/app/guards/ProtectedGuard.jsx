import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function ProtectedGuard() {
  const { isAuthenticated, authReady } = useAuth();
  if (authReady === false) {
    return <p className="muted" style={{ padding: '2rem' }}>Vérification de la session…</p>;
  }
  return isAuthenticated ? <Outlet /> : <Navigate to="/admin/login" replace />;
}
