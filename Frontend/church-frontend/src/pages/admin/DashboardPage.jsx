import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import { useTenant } from '../../hooks/useTenant';
import { requestService } from '../../services/request.service';
import { formatCurrency } from '../../utils/formatCurrency';
import { mapDemandeToRequestRow } from '../../utils/apiMappers';

export default function DashboardPage() {
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

  const stats = useMemo(() => {
    const total = rows.length;
    const pending = rows.filter((r) => r.requestStatus === 'EN_ATTENTE').length;
    const validated = rows.filter((r) => r.requestStatus === 'VALIDEE').length;
    const volume = rows.reduce((acc, r) => acc + (Number(r.amount) || 0), 0);
    return { total, pending, validated, volume };
  }, [rows]);

  const recent = useMemo(() => rows.slice(0, 5), [rows]);

  return (
    <div className="stack">
      <PageHeader
        title="Dashboard paroisse"
        subtitle="Vue d’ensemble des demandes et indicateurs de la paroisse active."
      />
      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}
      <div className="card-grid">
        <AppCard title="Demandes totales">
          <div className="kpi">
            <strong>{stats.total}</strong>
            <span className="page-subtitle">Enregistrées pour cette paroisse</span>
          </div>
        </AppCard>
        <AppCard title="En attente">
          <div className="kpi">
            <strong>{stats.pending}</strong>
            <span className="page-subtitle">Statut demande EN_ATTENTE</span>
          </div>
        </AppCard>
        <AppCard title="Validées">
          <div className="kpi">
            <strong>{stats.validated}</strong>
            <span className="page-subtitle">Statut demande VALIDEE</span>
          </div>
        </AppCard>
        <AppCard title="Volume (montants)">
          <div className="kpi">
            <strong>{formatCurrency(stats.volume)}</strong>
            <span className="page-subtitle">Somme des montants des demandes affichées</span>
          </div>
        </AppCard>
      </div>
      <AppCard title="Demandes récentes" subtitle="Les 5 dernières entrées de la liste.">
        <table className="app-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Demandeur</th>
              <th>Type</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {recent.map((item) => (
              <tr key={item.id}>
                <td>
                  <Link to={`/admin/demandes/${item.id}`}>{item.trackingCode}</Link>
                </td>
                <td>{item.applicant}</td>
                <td>{item.requestType}</td>
                <td>{item.requestStatus}</td>
              </tr>
            ))}
            {!recent.length && !loading ? (
              <tr>
                <td colSpan={4} className="muted">
                  Aucune demande pour cette paroisse.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </AppCard>
    </div>
  );
}
