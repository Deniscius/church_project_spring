import React, { Suspense, lazy } from 'react';
import { Outlet, Route } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import PublicDemandeDraftLayout from './PublicDemandeDraftLayout';
import PageSuspenseFallback from '../../components/ui/PageSuspenseFallback';
import HomePage from '../../pages/public/HomePage';

const NewRequestPage = lazy(() => import('../../pages/public/NewRequestPage'));
const RequestRecapPage = lazy(() => import('../../pages/public/RequestRecapPage'));
const RequestConfirmationPage = lazy(() => import('../../pages/public/RequestConfirmationPage'));
const TrackingPage = lazy(() => import('../../pages/public/TrackingPage'));
const TrackingResultPage = lazy(() => import('../../pages/public/TrackingResultPage'));
const PublicInvoicePage = lazy(() => import('../../pages/public/PublicInvoicePage'));
const PublicPaymentPage = lazy(() => import('../../pages/public/PublicPaymentPage'));
const PaymentReturnPage = lazy(() => import('../../pages/public/PaymentReturnPage'));
const ParishRegistrationPage = lazy(() => import('../../pages/public/ParishRegistrationPage'));
const PublicSchedulesPage = lazy(() => import('../../pages/public/PublicSchedulesPage'));
const SiteMapPage = lazy(() => import('../../pages/public/SiteMapPage'));

function LazyOutlet() {
  return (
    <Suspense fallback={<PageSuspenseFallback />}>
      <Outlet />
    </Suspense>
  );
}

export function PublicRoutes() {
  return (
    <Route element={<PublicLayout />}>
      {/* Accueil eager : LCP sans attendre un chunk lazy */}
      <Route path="/" element={<HomePage />} />
      <Route element={<LazyOutlet />}>
        <Route element={<PublicDemandeDraftLayout />}>
          <Route path="/demande" element={<NewRequestPage />} />
          <Route path="/demande/recapitulatif" element={<RequestRecapPage />} />
          <Route path="/demande/confirmation" element={<RequestConfirmationPage />} />
        </Route>
        <Route path="/suivi" element={<TrackingPage />} />
        <Route path="/suivi/resultat" element={<TrackingResultPage />} />
        <Route path="/horaires" element={<PublicSchedulesPage />} />
        <Route path="/facture" element={<PublicInvoicePage />} />
        <Route path="/facture/:codeSuivie" element={<PublicInvoicePage />} />
        <Route path="/paiement" element={<PublicPaymentPage />} />
        <Route path="/paiement/retour" element={<PaymentReturnPage />} />
        <Route path="/paiement/:codeSuivie" element={<PublicPaymentPage />} />
        <Route path="/inscription-paroisse" element={<ParishRegistrationPage />} />
        <Route path="/plan-du-site" element={<SiteMapPage />} />
      </Route>
    </Route>
  );
}
