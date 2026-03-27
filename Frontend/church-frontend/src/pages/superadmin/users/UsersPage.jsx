import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { mockUsers } from '../../../data/mockData';

const columns = [
  { key: 'firstName', label: 'Prénom' },
  { key: 'lastName', label: 'Nom' },
  { key: 'username', label: 'Username' },
  { key: 'role', label: 'Rôle' },
  { key: 'active', label: 'État' },
];

export default function UsersPage() {
  return (
    <div className="stack">
      <PageHeader title="Utilisateurs" subtitle="Gestion des comptes internes de l'application." />
      <AppTable
        columns={columns}
        rows={mockUsers}
        renderCell={(row, column) => column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]}
      />
    </div>
  );
}
