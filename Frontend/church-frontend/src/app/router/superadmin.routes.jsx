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

function PermissionRoute({ permission, path, element }) {
  return (
    <Route element={<PermissionGuard requiredPermissions={[permission]} />}>
      <Route path={path} element={element} />
    </Route>
  );
}

export function SuperAdminRoutes() {
  return (
    <Route element={<ProtectedGuard />}>
      <Route element={<PlatformStaffGuard />}>
        <Route element={<AdminLayout />}>
          <Route element={<LazyOutlet />}>
            <PermissionRoute
              permission={PERMISSIONS.PARISH_REGISTRATION_READ}
              path={ROUTES.PARISH_INSCRIPTIONS}
              element={<InscriptionsPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.FINANCE_READ}
              path={ROUTES.REVERSEMENTS}
              element={<ReversementsPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.SUBSCRIPTION_READ}
              path={ROUTES.SUBSCRIPTIONS}
              element={<AbonnementsPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.SCHEDULE_MANAGE}
              path={ROUTES.CATALOGUE_MODELE}
              element={<CatalogueModelePage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.DEMAND_AUDIT}
              path={ROUTES.PLATFORM_DEMANDES}
              element={<PlatformDemandesPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.SAAS_PLAN_READ}
              path={ROUTES.SAAS_PRICING}
              element={<SaasPricingPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.PARISH_READ}
              path={ROUTES.PARISHES}
              element={<ParishesPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.PARISH_MANAGE}
              path={ROUTES.PARISH_CREATE}
              element={<CreateParishPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.PARISH_MANAGE}
              path={ROUTES.PARISH_EDIT}
              element={<EditParishPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.PARISH_ACCESS_MANAGE}
              path={ROUTES.PARISH_ACCESS}
              element={<ParishAccessPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.DEANERY_MANAGE}
              path={ROUTES.DEANERIES}
              element={<DeaneriesPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.PAYMENT_TYPE_MANAGE}
              path={ROUTES.PAYMENT_TYPES}
              element={<PaymentTypesPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.USER_MANAGE}
              path={ROUTES.USERS}
              element={<UsersPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.USER_MANAGE}
              path={ROUTES.USER_CREATE}
              element={<CreateUserPage />}
            />
            <PermissionRoute
              permission={PERMISSIONS.USER_MANAGE}
              path={ROUTES.USER_EDIT}
              element={<EditUserPage />}
            />
          </Route>
        </Route>
      </Route>
    </Route>
  );
}
