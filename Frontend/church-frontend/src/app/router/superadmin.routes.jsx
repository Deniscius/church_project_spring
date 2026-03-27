import React from 'react';
import { Route } from 'react-router-dom';
import ProtectedGuard from '../guards/ProtectedGuard';
import AdminLayout from '../../layouts/AdminLayout';
import RoleGuard from '../guards/RoleGuard';
import ParishesPage from '../../pages/superadmin/parishes/ParishesPage';
import CreateParishPage from '../../pages/superadmin/parishes/CreateParishPage';
import EditParishPage from '../../pages/superadmin/parishes/EditParishPage';
import UsersPage from '../../pages/superadmin/users/UsersPage';
import CreateUserPage from '../../pages/superadmin/users/CreateUserPage';
import EditUserPage from '../../pages/superadmin/users/EditUserPage';
import ParishAccessPage from '../../pages/superadmin/parish-access/ParishAccessPage';
import LocalitiesPage from '../../pages/superadmin/localities/LocalitiesPage';
import PaymentTypesPage from '../../pages/superadmin/payment-types/PaymentTypesPage';

export function SuperAdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      <Route element={<RoleGuard allowedRoles={['ADMIN', 'SUPER_ADMIN']} />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin/paroisses" element={<ParishesPage />} />
          <Route path="/admin/paroisses/nouvelle" element={<CreateParishPage />} />
          <Route path="/admin/paroisses/:id/modifier" element={<EditParishPage />} />
          <Route path="/admin/utilisateurs" element={<UsersPage />} />
          <Route path="/admin/utilisateurs/nouveau" element={<CreateUserPage />} />
          <Route path="/admin/utilisateurs/:id/modifier" element={<EditUserPage />} />
          <Route path="/admin/acces-paroisses" element={<ParishAccessPage />} />
          <Route path="/admin/localites" element={<LocalitiesPage />} />
          <Route path="/admin/types-paiement" element={<PaymentTypesPage />} />
        </Route>
      </Route>
    </Route>
  );
}
