import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockPayments } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'type', label: 'Type' },
  { key: 'amount', label: 'Montant' },
  { key: 'status', label: 'Statut' },
  { key: 'paidAt', label: 'Date' },
];

export default function PaymentsPage() {
  return (
    <div className="stack">
      <PageHeader title="Paiements" subtitle="Vue de suivi des transactions liées aux demandes de la paroisse." />
      <AppTable
        columns={columns}
        rows={mockPayments}
        renderCell={(row, column) => {
          if (column.key === 'status') return <AppBadge value={row.status} />;
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'paidAt') return formatDate(row.paidAt);
          return row[column.key];
        }}
      />
    </div>
  );
}
