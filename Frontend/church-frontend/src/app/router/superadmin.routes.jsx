import React, { Suspense, lazy } from 'react';
import { Outlet, Route } from 'react-router-dom';
import ProtectedGuard from '../guards/ProtectedGuard';
import AdminLayout from '../../layouts/AdminLayout';
import GlobalAdminGuard from '../guards/GlobalAdminGuard';
import PlatformStaffGuard from '../guards/PlatformStaffGuard';
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
            {/* Finances SaaS + audit : SUPER_ADMIN + COMPTABLE */}
            <Route path={ROUTES.PARISH_INSCRIPTIONS} element={<InscriptionsPage />} />
            <Route path={ROUTES.REVERSEMENTS} element={<ReversementsPage />} />
            <Route path={ROUTES.SUBSCRIPTIONS} element={<AbonnementsPage />} />
            <Route path={ROUTES.CATALOGUE_MODELE} element={<CatalogueModelePage />} />
            <Route path={ROUTES.PLATFORM_DEMANDES} element={<PlatformDemandesPage />} />

            {/* Configuration système : SUPER_ADMIN uniquement */}
            <Route element={<GlobalAdminGuard />}>
              <Route path={ROUTES.SAAS_PRICING} element={<SaasPricingPage />} />
              <Route path={ROUTES.PARISHES} element={<ParishesPage />} />
              <Route path={ROUTES.PARISH_CREATE} element={<CreateParishPage />} />
              <Route path={ROUTES.PARISH_EDIT} element={<EditParishPage />} />
              <Route path={ROUTES.PARISH_ACCESS} element={<ParishAccessPage />} />
              <Route path={ROUTES.DEANERIES} element={<DeaneriesPage />} />
              <Route path={ROUTES.PAYMENT_TYPES} element={<PaymentTypesPage />} />
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
