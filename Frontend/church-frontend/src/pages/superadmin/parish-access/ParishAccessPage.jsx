import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockAccesses } from '../../../data/mockData';

const columns = [
  { key: 'user', label: 'Utilisateur' },
  { key: 'parish', label: 'Paroisse' },
  { key: 'role', label: 'Rôle' },
  { key: 'active', label: 'État' },
];

export default function ParishAccessPage() {
  return (
    <div className="stack">
      <PageHeader title="Accès paroisses" subtitle="Table de correspondance utilisateur ↔ paroisse ↔ rôle." />
      <AppTable
        columns={columns}
        rows={mockAccesses}
        renderCell={(row, column) => column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]}
      />
    </div>
  );
}
