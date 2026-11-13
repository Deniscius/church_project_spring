import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function ProtectedGuard() {
  const { isAuthenticated, authReady, sessionExpired, sessionError } = useAuth();
  const location = useLocation();
  if (authReady === false) {
    return <p className="muted" style={{ padding: '2rem' }}>Vérification de la session…</p>;
  }
  if (sessionError) {
    return <div className="page-section" role="alert"><p>{sessionError}</p><button type="button" onClick={() => window.location.reload()}>Réessayer</button></div>;
  }
  return isAuthenticated ? (
    <Outlet />
  ) : (
    <Navigate
      to="/admin/login"
      replace
      state={{
        from: `${location.pathname}${location.search}`,
        sessionExpired: Boolean(sessionExpired),
      }}
    />
  );
}
