import React from 'react';
import { Route } from 'react-router-dom';
import AuthLayout from '../../layouts/AuthLayout';
import LoginPage from '../../pages/auth/LoginPage';
import ForgotPasswordPage from '../../pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '../../pages/auth/ResetPasswordPage';

export function AuthRoutes() {
  return (
    <Route element={<AuthLayout />}>
      <Route path="/admin/login" element={<LoginPage />} />
      <Route path="/admin/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/admin/reset-password" element={<ResetPasswordPage />} />
    </Route>
  );
}
