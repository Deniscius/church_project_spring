import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import PricingForm from './PricingForm';

export default function EditPricingPage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader title="Modifier un forfait" subtitle="Formulaire d'édition d'un tarif existant." />
      <PricingForm pricingId={id} />
    </div>
  );
}
