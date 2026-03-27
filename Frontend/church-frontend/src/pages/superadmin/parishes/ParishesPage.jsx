import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockParishes } from '../../../data/mockData';

const columns = [
  { key: 'name', label: 'Paroisse' },
  { key: 'city', label: 'Localité' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Téléphone' },
  { key: 'active', label: 'État' },
];

export default function ParishesPage() {
  return (
    <div className="stack">
      <PageHeader title="Paroisses" subtitle="Administration globale des paroisses du système." />
      <AppTable
        columns={columns}
        rows={mockParishes}
        renderCell={(row, column) => column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]}
      />
    </div>
  );
}
