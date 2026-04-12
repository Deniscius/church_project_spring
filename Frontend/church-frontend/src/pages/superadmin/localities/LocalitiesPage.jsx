import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import { localityService } from '../../../services/locality.service';
import { mapLocaliteToRow } from '../../../utils/apiMappers';

const columns = [{ key: 'label', label: 'Localité' }];

export default function LocalitiesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await localityService.getAll();
        if (!cancelled) setRows((data || []).map(mapLocaliteToRow));
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
      <PageHeader title="Localités" subtitle="Référentiel géographique (API /localites)." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable columns={columns} rows={rows} />
    </div>
  );
}
