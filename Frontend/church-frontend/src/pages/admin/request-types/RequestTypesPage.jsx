import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { requestTypeService } from '../../../services/requestType.service';
import { mapTypeDemandeToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'category', label: 'Catégorie' },
  { key: 'active', label: 'État' },
];

export default function RequestTypesPage() {
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
        const data = await requestTypeService.getByParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapTypeDemandeToRow));
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
      <PageHeader title="Types de demande" subtitle="Référentiel pour la paroisse active." />
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
