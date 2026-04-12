import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import { paymentTypeService } from '../../../services/paymentType.service';
import { mapTypePaiementToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'mode', label: 'Mode' },
];

export default function PaymentTypesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await paymentTypeService.getAll();
        if (!cancelled) setRows((data || []).map(mapTypePaiementToRow));
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
      <PageHeader title="Types de paiement" subtitle="Modes de paiement acceptés (API /type-paiement)." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable columns={columns} rows={rows} />
    </div>
  );
}
