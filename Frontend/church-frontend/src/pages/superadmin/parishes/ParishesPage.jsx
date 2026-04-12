import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { parishService } from '../../../services/parish.service';
import { mapParoisseToTableRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'name', label: 'Paroisse' },
  { key: 'city', label: 'Localité' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Téléphone' },
  { key: 'active', label: 'État' },
];

export default function ParishesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await parishService.getAll();
        if (!cancelled) setRows((data || []).map(mapParoisseToTableRow));
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
      <PageHeader title="Paroisses" subtitle="Liste globale (API /paroisses)." />
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
