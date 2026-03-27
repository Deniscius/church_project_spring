import React from 'react';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import ApplicantForm from '../../components/public/ApplicantForm';
import RequestSummaryCard from '../../components/public/RequestSummaryCard';
import ParishSelector from '../../components/public/ParishSelector';
import RequestTypeSelector from '../../components/public/RequestTypeSelector';
import PricingSelector from '../../components/public/PricingSelector';
import ScheduleSelector from '../../components/public/ScheduleSelector';
import DatesSelector from '../../components/public/DatesSelector';
import PaymentTypeSelector from '../../components/public/PaymentTypeSelector';
import AppButton from '../../components/ui/AppButton';

export default function NewRequestPage() {
  return (
    <div className="stack">
      <PageHeader title="Nouvelle demande" subtitle="Page publique de création de demande, alimentée par les référentiels du backend." />
      <div className="grid-2">
        <div className="stack">
          <ApplicantForm />
          <ParishSelector />
          <RequestTypeSelector />
          <PricingSelector />
          <ScheduleSelector />
          <DatesSelector />
          <PaymentTypeSelector />
        </div>
        <div className="stack">
          <RequestSummaryCard />
          <AppCard title="Actions" subtitle="Flux recommandé">
            <div className="button-row">
              <AppButton>Continuer vers le récapitulatif</AppButton>
              <AppButton variant="secondary">Réinitialiser</AppButton>
            </div>
          </AppCard>
        </div>
      </div>
    </div>
  );
}
