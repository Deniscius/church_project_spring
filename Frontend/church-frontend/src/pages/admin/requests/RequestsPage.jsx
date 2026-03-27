import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockRequests } from '../../../data/mockData';
import { formatDate } from '../../../utils/formatDate';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut demande' },
  { key: 'paymentStatus', label: 'Statut paiement' },
  { key: 'createdAt', label: 'Date' },
];

export default function RequestsPage() {
  return (
    <div className="stack">
      <PageHeader title="Demandes" subtitle="Liste des demandes de la paroisse connectée avec filtres, lecture rapide et actions futures." />
      <div className="card filters">
        <select className="select"><option>Toutes les demandes</option></select>
        <select className="select"><option>Tous les statuts</option></select>
        <select className="select"><option>Tous les paiements</option></select>
        <input className="input" placeholder="Rechercher par code ou demandeur" />
      </div>
      <AppTable
        columns={columns}
        rows={mockRequests}
        renderCell={(row, column) => {
          if (column.key === 'requestStatus' || column.key === 'paymentStatus') {
            return <AppBadge value={row[column.key]} />;
          }
          if (column.key === 'createdAt') {
            return formatDate(row.createdAt);
          }
          return row[column.key];
        }}
      />
    </div>
  );
}
