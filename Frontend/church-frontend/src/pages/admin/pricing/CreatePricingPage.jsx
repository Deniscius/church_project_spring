import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import PricingForm from './PricingForm';

export default function CreatePricingPage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un forfait" subtitle="Formulaire de création des tarifs associés aux types de demande." />
      <PricingForm />
    </div>
  );
}
