import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { requestService } from '../../../services/request.service';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToRequestRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut demande' },
  { key: 'paymentStatus', label: 'Statut paiement' },
  { key: 'createdAt', label: 'Date' },
];

export default function RequestsPage() {
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
        const data = await requestService.getByParish(activeParish.id);
        if (!cancelled) setRows((data || []).map(mapDemandeToRequestRow));
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
      <PageHeader
        title="Demandes"
        subtitle="Liste des demandes de la paroisse active (données API)."
      />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <div className="card filters">
        <select className="select">
          <option>Toutes les demandes</option>
        </select>
        <select className="select">
          <option>Tous les statuts</option>
        </select>
        <select className="select">
          <option>Tous les paiements</option>
        </select>
        <input className="input" placeholder="Rechercher par code ou demandeur" readOnly />
      </div>
      <AppTable
        columns={columns}
        rows={rows}
        renderCell={(row, column) => {
          if (column.key === 'trackingCode') {
            return (
              <Link to={`/admin/demandes/${row.id}`}>{row.trackingCode}</Link>
            );
          }
          if (column.key === 'requestStatus' || column.key === 'paymentStatus') {
            return <AppBadge value={row[column.key]} />;
          }
          if (column.key === 'createdAt') {
            return formatDate(row.createdAt);
          }
          return row[column.key];
        }}
      />
    </div>
  );
}
