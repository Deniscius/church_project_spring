import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToPaymentRow } from '../../../utils/apiMappers';
import { useParishDemandesPage } from '../../../hooks/queries/useParishDemandes';

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'type', label: 'Mode' },
  { key: 'amount', label: 'Montant' },
  { key: 'status', label: 'Statut' },
  { key: 'paidAt', label: 'Date' },
];

const STATUS_OPTIONS = [
  { value: '', label: 'Tous les statuts' },
  { value: 'NON_PAYE', label: 'Non payé' },
  { value: 'EN_ATTENTE', label: 'En attente' },
  { value: 'PAYE', label: 'Payé' },
  { value: 'ECHOUE', label: 'Échoué' },
];

const PAGE_SIZE = 30;

export default function PaymentsPage() {
  const { activeParish } = useTenant();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [modeFilter, setModeFilter] = useState('');
  const [search, setSearch] = useState('');

  const { data, isLoading, error, isFetching } = useParishDemandesPage(
    activeParish?.id,
    page,
    PAGE_SIZE
  );

  const pageContent = useMemo(() => {
    const list = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : []);
    return list.map(mapDemandeToPaymentRow);
  }, [data]);

  const totalPages = data?.totalPages ?? (pageContent.length < PAGE_SIZE && page === 0 ? 1 : page + 2);
  const totalElements = data?.totalElements ?? pageContent.length;

  const modeOptions = useMemo(() => {
    const set = new Set();
    pageContent.forEach((row) => {
      const mode = String(row.type || '').trim();
      if (mode && mode !== '—') set.add(mode);
    });
    return [...set].sort((a, b) => a.localeCompare(b, 'fr'));
  }, [pageContent]);

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return pageContent.filter((row) => {
      if (statusFilter && row.status !== statusFilter) return false;
      if (modeFilter && row.type !== modeFilter) return false;
      if (!q) return true;
      return String(row.trackingCode || '').toLowerCase().includes(q)
        || String(row.applicant || '').toLowerCase().includes(q)
        || String(row.transactionId || '').toLowerCase().includes(q);
    });
  }, [pageContent, statusFilter, modeFilter, search]);

  return (
    <div className="stack">
      <PageHeader
        title="Paiements"
        subtitle="Synthèse paginée des règlements — filtrez par statut, mode ou recherche."
      />
      {error ? <p className="text-red-600">{error.message || 'Erreur'}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}

      <div className="filter-chips" role="group" aria-label="Statut de paiement">
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value || 'ALL'}
            type="button"
            className={`chip${statusFilter === opt.value ? ' is-active' : ''}`}
            aria-pressed={statusFilter === opt.value}
            onClick={() => {
              setStatusFilter(opt.value);
              setPage(0);
            }}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <div className="card filters">
        <select
          className="select"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(0);
          }}
          aria-label="Filtrer par statut de paiement"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value || 'all'} value={opt.value}>{opt.label}</option>
          ))}
        </select>
        <select
          className="select"
          value={modeFilter}
          onChange={(e) => {
            setModeFilter(e.target.value);
            setPage(0);
          }}
          aria-label="Filtrer par mode de paiement"
        >
          <option value="">Tous les modes</option>
          {modeOptions.map((mode) => (
            <option key={mode} value={mode}>{mode}</option>
          ))}
        </select>
        <input
          className="input"
          placeholder="Rechercher (code, demandeur, transaction)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <AppTable
        columns={columns}
        rows={rows}
        emptyMessage="Aucun paiement pour ces filtres."
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

      <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="muted" style={{ margin: 0 }}>
          Page {page + 1}
          {totalElements != null ? ` · ${totalElements} au total` : ''}
          {isFetching && !isLoading ? ' · actualisation…' : ''}
        </p>
        <div className="button-row">
          <button
            type="button"
            className="btn btn-secondary"
            disabled={page <= 0 || isLoading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Précédent
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            disabled={isLoading || page + 1 >= totalPages || pageContent.length < PAGE_SIZE}
            onClick={() => setPage((p) => p + 1)}
          >
            Suivant
          </button>
        </div>
      </div>
    </div>
  );
}
