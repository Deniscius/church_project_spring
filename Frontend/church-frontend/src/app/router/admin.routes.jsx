import React from 'react';
import { Route } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import ProtectedGuard from '../guards/ProtectedGuard';
import TenantGuard from '../guards/TenantGuard';
import PermissionGuard from '../guards/PermissionGuard';
import { PERMISSIONS } from '../../constants/roles';
import DashboardPage from '../../pages/admin/DashboardPage';
import RequestsPage from '../../pages/admin/requests/RequestsPage';
import RequestDetailsPage from '../../pages/admin/requests/RequestDetailsPage';
import EditRequestPage from '../../pages/admin/requests/EditRequestPage';
import PaymentsPage from '../../pages/admin/payments/PaymentsPage';
import PaymentDetailsPage from '../../pages/admin/payments/PaymentDetailsPage';
import InvoicesPage from '../../pages/admin/invoices/InvoicesPage';
import InvoiceDetailsPage from '../../pages/admin/invoices/InvoiceDetailsPage';
import SchedulesPage from '../../pages/admin/schedules/SchedulesPage';
import CreateSchedulePage from '../../pages/admin/schedules/CreateSchedulePage';
import EditSchedulePage from '../../pages/admin/schedules/EditSchedulePage';
import RequestTypesPage from '../../pages/admin/request-types/RequestTypesPage';
import CreateRequestTypePage from '../../pages/admin/request-types/CreateRequestTypePage';
import EditRequestTypePage from '../../pages/admin/request-types/EditRequestTypePage';
import PricingPage from '../../pages/admin/pricing/PricingPage';
import CreatePricingPage from '../../pages/admin/pricing/CreatePricingPage';
import EditPricingPage from '../../pages/admin/pricing/EditPricingPage';
import ProfilePage from '../../pages/admin/profile/ProfilePage';
import { UsersPage as TeamUsersPage } from '../../pages/admin/users/UsersPage';

export function AdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      <Route element={<TenantGuard />}>
        <Route element={<AdminLayout />}>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DASHBOARD_VIEW]} />}>
            <Route path="/admin/dashboard" element={<DashboardPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEMAND_READ]} />}>
            <Route path="/admin/demandes" element={<RequestsPage />} />
            <Route path="/admin/demandes/:id" element={<RequestDetailsPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEMAND_EDIT]} />}>
            <Route path="/admin/demandes/:id/modifier" element={<EditRequestPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PAYMENT_READ]} />}>
            <Route path="/admin/paiements" element={<PaymentsPage />} />
            <Route path="/admin/paiements/:id" element={<PaymentDetailsPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.INVOICE_READ]} />}>
            <Route path="/admin/factures" element={<InvoicesPage />} />
            <Route path="/admin/factures/:id" element={<InvoiceDetailsPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SCHEDULE_READ]} />}>
            <Route path="/admin/horaires" element={<SchedulesPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SCHEDULE_MANAGE]} />}>
            <Route path="/admin/horaires/nouveau" element={<CreateSchedulePage />} />
            <Route path="/admin/horaires/:id/modifier" element={<EditSchedulePage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.REQUEST_TYPE_READ]} />}>
            <Route path="/admin/types-demandes" element={<RequestTypesPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.REQUEST_TYPE_MANAGE]} />}>
            <Route path="/admin/types-demandes/nouveau" element={<CreateRequestTypePage />} />
            <Route path="/admin/types-demandes/:id/modifier" element={<EditRequestTypePage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PRICING_READ]} />}>
            <Route path="/admin/forfaits" element={<PricingPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PRICING_MANAGE]} />}>
            <Route path="/admin/forfaits/nouveau" element={<CreatePricingPage />} />
            <Route path="/admin/forfaits/:id/modifier" element={<EditPricingPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.USER_MANAGE]} />}>
            <Route path="/admin/equipe" element={<TeamUsersPage />} />
          </Route>
          <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PROFILE_READ]} />}>
            <Route path="/admin/profil" element={<ProfilePage />} />
          </Route>
        </Route>
      </Route>
    </Route>
  );
}
