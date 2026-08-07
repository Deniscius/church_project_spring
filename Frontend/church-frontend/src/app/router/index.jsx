import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { PublicRoutes } from './public.routes';
import { AuthRoutes } from './auth.routes';
import { AdminRoutes } from './admin.routes';
import { SuperAdminRoutes } from './superadmin.routes';
import PublicLayout from '../../layouts/PublicLayout';
import UnauthorizedPage from '../../pages/UnauthorizedPage';
import NotFoundPage from '../../pages/NotFoundPage';

export default function AppRouter() {
  return (
    <Routes>
      {PublicRoutes()}
      {AuthRoutes()}
      {AdminRoutes()}
      {SuperAdminRoutes()}
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route element={<PublicLayout />}>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
