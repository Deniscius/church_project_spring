import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { paymentService } from '../../../services/payment.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToPaymentRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'type', label: 'Type' },
  { key: 'amount', label: 'Montant' },
  { key: 'status', label: 'Statut' },
  { key: 'paidAt', label: 'Date' },
];

export default function PaymentsPage() {
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
        const data = await paymentService.listRowsForParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapDemandeToPaymentRow));
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
      <PageHeader title="Paiements" subtitle="Synthèse à partir des demandes de la paroisse (champs paiement)." />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'trackingCode') {
            return <Link to={`/admin/paiements/${row.id}`}>{row.trackingCode}</Link>;
          }
          if (column.key === 'status') return <AppBadge value={row.status} />;
          if (column.key === 'amount') return formatCurrency(row.amount);
          if (column.key === 'paidAt') return formatDate(row.paidAt);
          return row[column.key];
        }}
      />
    </div>
  );
}
