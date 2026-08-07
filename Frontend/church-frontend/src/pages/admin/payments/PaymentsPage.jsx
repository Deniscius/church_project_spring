import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToPaymentRow } from '../../../utils/apiMappers';
import { useParishDemandes } from '../../../hooks/queries/useParishDemandes';

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
  const { data, isLoading, error } = useParishDemandes(activeParish?.id);
  const rows = useMemo(() => (data || []).map(mapDemandeToPaymentRow), [data]);

  return (
    <div className="stack">
      <PageHeader title="Paiements" subtitle="Synthèse à partir des demandes de la paroisse (champs paiement)." />
      {error ? <p className="text-red-600">{error.message || 'Erreur'}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'trackingCode') {
            return <Link to={`/admin/paiements/${row.id}`}>{row.trackingCode}</Link>;
          }
          if (column.key === 'amount') return formatCurrency(row.amount || 0);
          if (column.key === 'status') return <AppBadge value={row.status} />;
          if (column.key === 'paidAt') return formatDate(row.paidAt);
          return row[column.key];
        }}
      />
    </div>
  );
}
