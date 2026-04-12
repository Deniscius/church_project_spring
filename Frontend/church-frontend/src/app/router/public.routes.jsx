import React from 'react';
import { Route } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import PublicDemandeDraftLayout from './PublicDemandeDraftLayout';
import HomePage from '../../pages/public/HomePage';
import NewRequestPage from '../../pages/public/NewRequestPage';
import RequestRecapPage from '../../pages/public/RequestRecapPage';
import RequestConfirmationPage from '../../pages/public/RequestConfirmationPage';
import TrackingPage from '../../pages/public/TrackingPage';
import TrackingResultPage from '../../pages/public/TrackingResultPage';
import PublicInvoicePage from '../../pages/public/PublicInvoicePage';
import PublicPaymentPage from '../../pages/public/PublicPaymentPage';

export function PublicRoutes() {
  return (
    <Route element={<PublicLayout />}>
      <Route path="/" element={<HomePage />} />
      <Route element={<PublicDemandeDraftLayout />}>
        <Route path="/demande" element={<NewRequestPage />} />
        <Route path="/demande/recapitulatif" element={<RequestRecapPage />} />
        <Route path="/demande/confirmation" element={<RequestConfirmationPage />} />
      </Route>
      <Route path="/suivi" element={<TrackingPage />} />
      <Route path="/suivi/resultat" element={<TrackingResultPage />} />
      <Route path="/facture/:codeSuivie" element={<PublicInvoicePage />} />
      <Route path="/paiement/:codeSuivie" element={<PublicPaymentPage />} />
    </Route>
  );
}
