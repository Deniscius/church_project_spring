import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import { mockLocalities } from '../../../data/mockData';

const columns = [
  { key: 'label', label: 'Localité' },
];

export default function LocalitiesPage() {
  return (
    <div className="stack">
      <PageHeader title="Localités" subtitle="Référentiel des localités utilisées par les paroisses." />
      <AppTable columns={columns} rows={mockLocalities} />
    </div>
  );
}
