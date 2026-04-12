import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { userService } from '../../../services/user.service';
import { mapUserToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'firstName', label: 'Prénom' },
  { key: 'lastName', label: 'Nom' },
  { key: 'username', label: 'Username' },
  { key: 'role', label: 'Rôle' },
  { key: 'active', label: 'État' },
];

export default function UsersPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await userService.getAll();
        if (!cancelled) setRows((data || []).map(mapUserToRow));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="stack">
      <PageHeader title="Utilisateurs" subtitle="Comptes internes (API /users)." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) =>
          column.key === 'active' ? <AppBadge value={row.active} /> : row[column.key]
        }
      />
    </div>
  );
}
