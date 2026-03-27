import React from 'react';
import { Route } from 'react-router-dom';
import AuthLayout from '../../layouts/AuthLayout';
import LoginPage from '../../pages/auth/LoginPage';

export function AuthRoutes() {
  return (
    <Route element={<AuthLayout />}>
      <Route path="/admin/login" element={<LoginPage />} />
    </Route>
  );
}
