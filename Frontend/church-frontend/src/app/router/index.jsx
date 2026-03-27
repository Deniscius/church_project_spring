import React from 'react';
import { Navigate, Routes, Route } from 'react-router-dom';
import { PublicRoutes } from './public.routes';
import { AuthRoutes } from './auth.routes';
import { AdminRoutes } from './admin.routes';
import { SuperAdminRoutes } from './superadmin.routes';

function NotFound() {
  return <Navigate to="/" replace />;
}

export default function AppRouter() {
  return (
    <Routes>
      {PublicRoutes()}
      {AuthRoutes()}
      {AdminRoutes()}
      {SuperAdminRoutes()}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
