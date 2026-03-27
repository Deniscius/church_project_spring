import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockSchedules } from '../../../data/mockData';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'day', label: 'Jour' },
  { key: 'hour', label: 'Heure' },
  { key: 'active', label: 'État' },
];

export default function SchedulesPage() {
  return (
    <div className="stack">
      <PageHeader title="Horaires" subtitle="Paramétrage des horaires de célébration pour la paroisse connectée." />
      <AppTable
        columns={columns}
        rows={mockSchedules}
        renderCell={(row, column) => column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]}
      />
    </div>
  );
}
