import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockRequestTypes } from '../../../data/mockData';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'category', label: 'Catégorie' },
  { key: 'active', label: 'État' },
];

export default function RequestTypesPage() {
  return (
    <div className="stack">
      <PageHeader title="Types de demande" subtitle="Référentiel métier par paroisse." />
      <AppTable
        columns={columns}
        rows={mockRequestTypes}
        renderCell={(row, column) => column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]}
      />
    </div>
  );
}
