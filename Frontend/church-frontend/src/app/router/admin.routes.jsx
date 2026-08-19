import React, { Suspense, lazy } from 'react';
import { Outlet, Route } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import ProtectedGuard from '../guards/ProtectedGuard';
import TenantGuard from '../guards/TenantGuard';
import PermissionGuard from '../guards/PermissionGuard';
import { PERMISSIONS } from '../../constants/roles';
import { ROUTES } from '../../constants/routes';
import PageSuspenseFallback from '../../components/ui/PageSuspenseFallback';

const DashboardPage = lazy(() => import('../../pages/admin/DashboardPage'));
const DailyProgrammePage = lazy(() => import('../../pages/admin/celebrations/DailyProgrammePage'));
const RequestsPage = lazy(() => import('../../pages/admin/requests/RequestsPage'));
const RequestDetailsPage = lazy(() => import('../../pages/admin/requests/RequestDetailsPage'));
const EditRequestPage = lazy(() => import('../../pages/admin/requests/EditRequestPage'));
const CelebrationSheetPage = lazy(() => import('../../pages/admin/celebrations/CelebrationSheetPage'));
const PaymentsPage = lazy(() => import('../../pages/admin/payments/PaymentsPage'));
const PaymentDetailsPage = lazy(() => import('../../pages/admin/payments/PaymentDetailsPage'));
const InvoicesPage = lazy(() => import('../../pages/admin/invoices/InvoicesPage'));
const InvoiceDetailsPage = lazy(() => import('../../pages/admin/invoices/InvoiceDetailsPage'));
const SchedulesPage = lazy(() => import('../../pages/admin/schedules/SchedulesPage'));
const CreateSchedulePage = lazy(() => import('../../pages/admin/schedules/CreateSchedulePage'));
const EditSchedulePage = lazy(() => import('../../pages/admin/schedules/EditSchedulePage'));
const RequestTypesPage = lazy(() => import('../../pages/admin/request-types/RequestTypesPage'));
const CreateRequestTypePage = lazy(() => import('../../pages/admin/request-types/CreateRequestTypePage'));
const EditRequestTypePage = lazy(() => import('../../pages/admin/request-types/EditRequestTypePage'));
const PricingPage = lazy(() => import('../../pages/admin/pricing/PricingPage'));
const CreatePricingPage = lazy(() => import('../../pages/admin/pricing/CreatePricingPage'));
const EditPricingPage = lazy(() => import('../../pages/admin/pricing/EditPricingPage'));
const ProfilePage = lazy(() => import('../../pages/admin/profile/ProfilePage'));
const UsersPage = lazy(() => import('../../pages/admin/users/UsersPage'));
const CreateTeamMemberPage = lazy(() => import('../../pages/admin/users/CreateTeamMemberPage'));
const EditTeamMemberPage = lazy(() => import('../../pages/admin/users/EditTeamMemberPage'));
const ParishTreasuryPage = lazy(() => import('../../pages/admin/treasury/ParishTreasuryPage'));
const ReceiptSettingsPage = lazy(() => import('../../pages/admin/receipt/ReceiptSettingsPage'));

function LazyOutlet() {
  return (
    <Suspense fallback={<PageSuspenseFallback />}>
      <Outlet />
    </Suspense>
  );
}

export function AdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      {/* Le libre-service du compte ne dépend d'aucune paroisse : l'équipe
          plateforme (super admin, comptable) doit y accéder aussi. */}
      <Route element={<AdminLayout />}>
        <Route element={<LazyOutlet />}>
          <Route path={ROUTES.PROFILE} element={<ProfilePage />} />
        </Route>
      </Route>
      <Route element={<TenantGuard />}>
        <Route element={<AdminLayout />}>
          <Route element={<LazyOutlet />}>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DASHBOARD_VIEW]} />}>
              <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEMAND_READ]} />}>
              <Route path={ROUTES.DAILY_PROGRAMME} element={<DailyProgrammePage />} />
              <Route path={ROUTES.REQUESTS} element={<RequestsPage />} />
              <Route path={ROUTES.REQUEST_DETAILS} element={<RequestDetailsPage />} />
              <Route path={ROUTES.CELEBRATION_SHEET} element={<CelebrationSheetPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEMAND_EDIT]} />}>
              <Route path={ROUTES.REQUEST_EDIT} element={<EditRequestPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PAYMENT_READ]} />}>
              <Route path={ROUTES.PAYMENTS} element={<PaymentsPage />} />
              <Route path={ROUTES.PAYMENT_DETAILS} element={<PaymentDetailsPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.INVOICE_READ]} />}>
              <Route path={ROUTES.INVOICES} element={<InvoicesPage />} />
              <Route path={ROUTES.INVOICE_DETAILS} element={<InvoiceDetailsPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SCHEDULE_READ]} />}>
              <Route path={ROUTES.SCHEDULES} element={<SchedulesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SCHEDULE_MANAGE]} />}>
              <Route path={ROUTES.SCHEDULE_CREATE} element={<CreateSchedulePage />} />
              <Route path={ROUTES.SCHEDULE_EDIT} element={<EditSchedulePage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.REQUEST_TYPE_READ]} />}>
              <Route path={ROUTES.REQUEST_TYPES} element={<RequestTypesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.REQUEST_TYPE_MANAGE]} />}>
              <Route path={ROUTES.REQUEST_TYPE_CREATE} element={<CreateRequestTypePage />} />
              <Route path={ROUTES.REQUEST_TYPE_EDIT} element={<EditRequestTypePage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PRICING_READ]} />}>
              <Route path={ROUTES.PRICING} element={<PricingPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PRICING_MANAGE]} />}>
              <Route path={ROUTES.PRICING_CREATE} element={<CreatePricingPage />} />
              <Route path={ROUTES.PRICING_EDIT} element={<EditPricingPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.USER_MANAGE]} />}>
              <Route path={ROUTES.TEAM} element={<UsersPage />} />
              <Route path={ROUTES.TEAM_CREATE} element={<CreateTeamMemberPage />} />
              <Route path={ROUTES.TEAM_EDIT} element={<EditTeamMemberPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.TREASURY_READ]} />}>
              <Route path={ROUTES.TREASURY} element={<ParishTreasuryPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.RECEIPT_MANAGE]} />}>
              <Route path={ROUTES.RECEIPT} element={<ReceiptSettingsPage />} />
            </Route>
          </Route>
        </Route>
      </Route>
    </Route>
  );
}
