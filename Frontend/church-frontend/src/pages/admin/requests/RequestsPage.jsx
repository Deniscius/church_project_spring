import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { requestService } from '../../../services/request.service';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToRequestRow } from '../../../utils/apiMappers';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut demande' },
  { key: 'validationStatus', label: 'Validation' },
  { key: 'paymentStatus', label: 'Statut paiement' },
  { key: 'createdAt', label: 'Date' },
  { key: 'actions', label: 'Actions' },
];

export default function RequestsPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [paymentFilter, setPaymentFilter] = useState('');
  const [search, setSearch] = useState('');

  const filteredRows = useMemo(() => rows.filter((row) => {
    const term = search.trim().toLocaleLowerCase('fr');
    const matchesSearch = !term
      || row.trackingCode?.toLocaleLowerCase('fr').includes(term)
      || row.applicant?.toLocaleLowerCase('fr').includes(term);
    return matchesSearch
      && (!statusFilter || row.requestStatus === statusFilter)
      && (!paymentFilter || row.paymentStatus === paymentFilter);
  }), [rows, search, statusFilter, paymentFilter]);

  const remove = async (id) => {
    if (!window.confirm('Supprimer cette demande ? Cette action la retirera des listes actives.')) return;
    try {
      setError(null);
      await requestService.remove(id);
      setRows((current) => current.filter((row) => row.id !== id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible');
    }
  };

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
        <select className="select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Tous les statuts</option>
          <option value="EN_ATTENTE">En attente</option>
          <option value="VALIDEE">Validée</option>
          <option value="REJETEE">Rejetée</option>
        </select>
        <select className="select" value={paymentFilter} onChange={(e) => setPaymentFilter(e.target.value)}>
          <option value="">Tous les paiements</option>
          <option value="NON_PAYE">Non payé</option>
          <option value="PAYE">Payé</option>
          <option value="ECHOUE">Échoué</option>
        </select>
        <input className="input" placeholder="Rechercher par code ou demandeur" value={search}
          onChange={(e) => setSearch(e.target.value)} />
      </div>
      <AppTable
        columns={columns}
        rows={filteredRows}
        renderCell={(row, column) => {
          if (column.key === 'trackingCode') {
            return (
              <Link to={`/admin/demandes/${row.id}`}>{row.trackingCode}</Link>
            );
          }
          if (column.key === 'requestStatus' || column.key === 'validationStatus' || column.key === 'paymentStatus') {
            return <AppBadge value={row[column.key]} />;
          }
          if (column.key === 'createdAt') {
            return formatDate(row.createdAt);
          }
          if (column.key === 'actions') {
            return (
              <div className="button-row">
                <Link className="btn btn-secondary" to={`/admin/demandes/${row.id}`}>Voir</Link>
                {has(PERMISSIONS.DEMAND_EDIT) ? (
                  <Link className="btn btn-secondary" to={`/admin/demandes/${row.id}/modifier`}>Modifier</Link>
                ) : null}
                {has(PERMISSIONS.DEMAND_DELETE) ? (
                  <button className="btn btn-danger" onClick={() => remove(row.id)}>Supprimer</button>
                ) : null}
              </div>
            );
          }
          return row[column.key];
        }}
      />
    </div>
  );
}
