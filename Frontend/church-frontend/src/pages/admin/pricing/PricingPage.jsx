import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { pricingService } from '../../../services/pricing.service';
import { requestTypeService } from '../../../services/requestType.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { mapForfaitToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'label', label: 'Forfait' },
  { key: 'amount', label: 'Montant' },
  { key: 'celebrations', label: 'Célébrations' },
  { key: 'customHour', label: 'Heure perso' },
  { key: 'active', label: 'État' },
];

export default function PricingPage() {
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
        const types = await requestTypeService.getByParish(activeParish.id);
        const all = await pricingService.getAll();
        if (cancelled) return;
        const typeSet = new Set((types || []).map((t) => t.publicId));
        const filtered = (all || []).filter((f) => typeSet.has(f.typeDemandePublicId));
        setRows(filtered.map(mapForfaitToRow));
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
      <PageHeader title="Forfaits" subtitle="Forfaits dont le type de demande appartient à la paroisse active." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'customHour') return row.customHour ? 'Oui' : 'Non';
          if (column.key === 'active') return <AppBadge value={row.active} />;
          return row[column.key];
        }}
      />
    </div>
  );
}
