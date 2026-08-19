import React, { Suspense, lazy } from 'react';
import { Outlet, Route } from 'react-router-dom';
import ProtectedGuard from '../guards/ProtectedGuard';
import PermissionGuard from '../guards/PermissionGuard';
import PlatformStaffGuard from '../guards/PlatformStaffGuard';
import AdminLayout from '../../layouts/AdminLayout';
import { PERMISSIONS } from '../../constants/roles';
import { ROUTES } from '../../constants/routes';
import PageSuspenseFallback from '../../components/ui/PageSuspenseFallback';

const ParishesPage = lazy(() => import('../../pages/superadmin/parishes/ParishesPage'));
const CreateParishPage = lazy(() => import('../../pages/superadmin/parishes/CreateParishPage'));
const EditParishPage = lazy(() => import('../../pages/superadmin/parishes/EditParishPage'));
const UsersPage = lazy(() => import('../../pages/superadmin/users/UsersPage'));
const CreateUserPage = lazy(() => import('../../pages/superadmin/users/CreateUserPage'));
const EditUserPage = lazy(() => import('../../pages/superadmin/users/EditUserPage'));
const ParishAccessPage = lazy(() => import('../../pages/superadmin/parish-access/ParishAccessPage'));
const DeaneriesPage = lazy(() => import('../../pages/superadmin/deaneries/DeaneriesPage'));
const PaymentTypesPage = lazy(() => import('../../pages/superadmin/payment-types/PaymentTypesPage'));
const InscriptionsPage = lazy(() => import('../../pages/superadmin/inscriptions/InscriptionsPage'));
const ReversementsPage = lazy(() => import('../../pages/superadmin/comptabilite/ReversementsPage'));
const AbonnementsPage = lazy(() => import('../../pages/superadmin/comptabilite/AbonnementsPage'));
const CatalogueModelePage = lazy(() => import('../../pages/superadmin/comptabilite/CatalogueModelePage'));
const PlatformDemandesPage = lazy(() => import('../../pages/superadmin/comptabilite/PlatformDemandesPage'));
const SaasPricingPage = lazy(() => import('../../pages/superadmin/settings/SaasPricingPage'));

function LazyOutlet() {
  return (
    <Suspense fallback={<PageSuspenseFallback />}>
      <Outlet />
    </Suspense>
  );
}

export function SuperAdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      <Route element={<PlatformStaffGuard />}>
        <Route element={<AdminLayout />}>
          <Route element={<LazyOutlet />}>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PARISH_REGISTRATION_READ]} />}>
              <Route path={ROUTES.PARISH_INSCRIPTIONS} element={<InscriptionsPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.FINANCE_READ]} />}>
              <Route path={ROUTES.REVERSEMENTS} element={<ReversementsPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SUBSCRIPTION_READ]} />}>
              <Route path={ROUTES.SUBSCRIPTIONS} element={<AbonnementsPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SCHEDULE_MANAGE]} />}>
              <Route path={ROUTES.CATALOGUE_MODELE} element={<CatalogueModelePage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEMAND_AUDIT]} />}>
              <Route path={ROUTES.PLATFORM_DEMANDES} element={<PlatformDemandesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.SAAS_PLAN_READ]} />}>
              <Route path={ROUTES.SAAS_PRICING} element={<SaasPricingPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PARISH_READ]} />}>
              <Route path={ROUTES.PARISHES} element={<ParishesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PARISH_MANAGE]} />}>
              <Route path={ROUTES.PARISH_CREATE} element={<CreateParishPage />} />
              <Route path={ROUTES.PARISH_EDIT} element={<EditParishPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PARISH_ACCESS_MANAGE]} />}>
              <Route path={ROUTES.PARISH_ACCESS} element={<ParishAccessPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.DEANERY_MANAGE]} />}>
              <Route path={ROUTES.DEANERIES} element={<DeaneriesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.PAYMENT_TYPE_MANAGE]} />}>
              <Route path={ROUTES.PAYMENT_TYPES} element={<PaymentTypesPage />} />
            </Route>
            <Route element={<PermissionGuard requiredPermissions={[PERMISSIONS.USER_MANAGE]} />}>
              <Route path={ROUTES.USERS} element={<UsersPage />} />
              <Route path={ROUTES.USER_CREATE} element={<CreateUserPage />} />
              <Route path={ROUTES.USER_EDIT} element={<EditUserPage />} />
            </Route>
          </Route>
        </Route>
      </Route>
    </Route>
  );
}
