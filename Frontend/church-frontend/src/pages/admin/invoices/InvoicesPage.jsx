import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockInvoices } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';

const columns = [
  { key: 'number', label: 'Facture' },
  { key: 'trackingCode', label: 'Code de suivi' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'amount', label: 'Montant' },
  { key: 'status', label: 'Statut' },
];

export default function InvoicesPage() {
  return (
    <div className="stack">
      <PageHeader title="Factures" subtitle="Liste des factures liées aux demandes de la paroisse." />
      <AppTable
        columns={columns}
        rows={mockInvoices}
        renderCell={(row, column) => {
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'status') return <AppBadge value={row.status} />;
          return row[column.key];
        }}
      />
    </div>
  );
}
