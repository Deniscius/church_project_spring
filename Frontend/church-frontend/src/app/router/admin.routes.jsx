import React from 'react';
import { Route } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import ProtectedGuard from '../guards/ProtectedGuard';
import TenantGuard from '../guards/TenantGuard';
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

export function AdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      <Route element={<TenantGuard />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin/dashboard" element={<DashboardPage />} />
          <Route path="/admin/demandes" element={<RequestsPage />} />
          <Route path="/admin/demandes/:id" element={<RequestDetailsPage />} />
          <Route path="/admin/demandes/:id/modifier" element={<EditRequestPage />} />
          <Route path="/admin/paiements" element={<PaymentsPage />} />
          <Route path="/admin/paiements/:id" element={<PaymentDetailsPage />} />
          <Route path="/admin/factures" element={<InvoicesPage />} />
          <Route path="/admin/factures/:id" element={<InvoiceDetailsPage />} />
          <Route path="/admin/horaires" element={<SchedulesPage />} />
          <Route path="/admin/horaires/nouveau" element={<CreateSchedulePage />} />
          <Route path="/admin/horaires/:id/modifier" element={<EditSchedulePage />} />
          <Route path="/admin/types-demandes" element={<RequestTypesPage />} />
          <Route path="/admin/types-demandes/nouveau" element={<CreateRequestTypePage />} />
          <Route path="/admin/types-demandes/:id/modifier" element={<EditRequestTypePage />} />
          <Route path="/admin/forfaits" element={<PricingPage />} />
          <Route path="/admin/forfaits/nouveau" element={<CreatePricingPage />} />
          <Route path="/admin/forfaits/:id/modifier" element={<EditPricingPage />} />
          <Route path="/admin/profil" element={<ProfilePage />} />
        </Route>
      </Route>
    </Route>
  );
}
