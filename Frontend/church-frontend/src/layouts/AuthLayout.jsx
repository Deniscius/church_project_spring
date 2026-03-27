import React from 'react';
import { Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div className="auth-shell">
      <div className="auth-card card">
        <Outlet />
      </div>
    </div>
  );
}
