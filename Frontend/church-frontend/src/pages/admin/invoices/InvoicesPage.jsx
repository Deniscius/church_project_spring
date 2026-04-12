import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { invoiceService } from '../../../services/invoice.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { mapFactureToInvoiceRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'number', label: 'Facture' },
  { key: 'trackingCode', label: 'Code de suivi' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'amount', label: 'Montant' },
  { key: 'status', label: 'Statut' },
];

export default function InvoicesPage() {
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
        const data = await invoiceService.listForParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapFactureToInvoiceRow));
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
      <PageHeader title="Factures" subtitle="Factures liées aux demandes de la paroisse active." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'number') {
            return <Link to={`/admin/factures/${row.id}`}>{row.number}</Link>;
          }
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'status') return <AppBadge value={row.status} />;
          return row[column.key];
        }}
      />
    </div>
  );
}
