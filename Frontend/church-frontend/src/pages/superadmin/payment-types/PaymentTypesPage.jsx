import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import { mockPaymentTypes } from '../../../data/mockData';

const columns = [
  { key: 'label', label: 'Type de paiement' },
];

export default function PaymentTypesPage() {
  return (
    <div className="stack">
      <PageHeader title="Types de paiement" subtitle="Référentiel global des moyens de paiement." />
      <AppTable columns={columns} rows={mockPaymentTypes} />
    </div>
  );
}
