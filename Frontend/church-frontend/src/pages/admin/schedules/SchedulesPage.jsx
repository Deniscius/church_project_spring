import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { scheduleService } from '../../../services/schedule.service';
import { mapHoraireToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'day', label: 'Jour' },
  { key: 'hour', label: 'Heure' },
  { key: 'active', label: 'État' },
];

export default function SchedulesPage() {
  const { activeParish } = useTenant();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!activeParish?.id) {
      setRows([]);
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await scheduleService.getByParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapHoraireToRow));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeParish?.id]);

  return (
    <div className="stack">
      <PageHeader title="Horaires" subtitle="Horaires de célébration de la paroisse active." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          return row[column.key];
        }}
      />
    </div>
  );
}
