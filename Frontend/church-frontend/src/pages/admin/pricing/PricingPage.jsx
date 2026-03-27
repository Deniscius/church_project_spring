import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockPricing } from '../../../data/mockData';
import { formatCurrency } from '../../../utils/formatCurrency';

const columns = [
  { key: 'label', label: 'Forfait' },
  { key: 'amount', label: 'Montant' },
  { key: 'celebrations', label: 'Célébrations' },
  { key: 'customHour', label: 'Heure perso' },
  { key: 'active', label: 'État' },
];

export default function PricingPage() {
  return (
    <div className="stack">
      <PageHeader title="Forfaits" subtitle="Gestion des forfaits et tarifs proposés par la paroisse." />
      <AppTable
        columns={columns}
        rows={mockPricing}
        renderCell={(row, column) => {
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'customHour') return row.customHour ? 'Oui' : 'Non';
          if (column.key === 'active') return <AppBadge value={row.active} />;
          return row[column.key];
        }}
      />
    </div>
  );
}
