import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { invoiceService } from '../../../services/invoice.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatDate } from '../../../utils/formatDate';
import { mapFactureToInvoiceRow } from '../../../utils/apiMappers';
import { paymentStatusLabel } from '../../../utils/statusMapper';

const columns = [
  { key: 'number', label: 'Facture' },
  { key: 'object', label: 'Objet' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'amount', label: 'Montant' },
  { key: 'netAmount', label: 'Net paroisse' },
  { key: 'status', label: 'Statut' },
  { key: 'paidAt', label: 'Réglée le' },
];

const STATUS_FILTERS = [
  { id: 'ALL', label: 'Toutes' },
  { id: 'PAYE', label: 'Payées' },
  { id: 'EN_ATTENTE', label: 'En cours' },
  { id: 'NON_PAYE', label: 'Non payées' },
  { id: 'ECHOUE', label: 'Échouées' },
];

function sum(rows, key) {
  return rows.reduce((total, row) => total + (row[key] || 0), 0);
}

export default function InvoicesPage() {
  const { activeParish } = useTenant();
  const [status, setStatus] = useState('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 30;

  const { data, isLoading, error } = useQuery({
    queryKey: ['factures', 'paroisse', activeParish?.id],
    queryFn: ({ signal }) => invoiceService.listForParish(activeParish.id, { signal }),
    enabled: Boolean(activeParish?.id),
  });

  const rows = useMemo(() => (data || []).map(mapFactureToInvoiceRow), [data]);

  const counts = useMemo(() => {
    const base = { ALL: rows.length };
    STATUS_FILTERS.slice(1).forEach((f) => {
      base[f.id] = rows.filter((row) => row.status === f.id).length;
    });
    return base;
  }, [rows]);

  const totals = useMemo(() => {
    const paid = rows.filter((row) => row.status === 'PAYE');
    return {
      billed: sum(rows, 'amount'),
      collected: sum(paid, 'amount'),
      // Le net est ce qui alimente réellement la trésorerie de la paroisse.
      net: sum(paid, 'netAmount'),
      fees: sum(paid, 'fees'),
      outstanding: sum(rows.filter((row) => row.status !== 'PAYE'), 'amount'),
    };
  }, [rows]);

  const visible = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return rows.filter((row) => {
      if (status !== 'ALL' && row.status !== status) return false;
      if (!needle) return true;
      return [row.number, row.trackingCode, row.applicant, row.intention, row.transactionId]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(needle));
    });
  }, [rows, status, search]);

  useEffect(() => {
    setPage(0);
  }, [status, search, activeParish?.id]);

  const pageCount = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount - 1);
  const pagedRows = visible.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);
  return (
    <div className="stack">
      <PageHeader
        title="Factures"
        subtitle={
          activeParish?.name
            ? `Toutes les intentions facturées — ${activeParish.name}`
            : 'Toutes les intentions facturées de la paroisse active.'
        }
      />

      {error ? <p className="alert-error">{error.message || 'Erreur'}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}

      <div className="card-grid">
        <AppCard title="Total facturé">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.billed)}</strong>
            <span className="page-subtitle">{rows.length} facture{rows.length > 1 ? 's' : ''} émise{rows.length > 1 ? 's' : ''}</span>
          </div>
        </AppCard>
        <AppCard title="Encaissé">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.collected)}</strong>
            <span className="page-subtitle">{counts.PAYE || 0} facture{(counts.PAYE || 0) > 1 ? 's' : ''} payée{(counts.PAYE || 0) > 1 ? 's' : ''}</span>
          </div>
        </AppCard>
        <AppCard title="Net paroisse">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.net)}</strong>
            <span className="page-subtitle">
              Après {formatCurrency(totals.fees)} de frais — c’est ce montant qui alimente la trésorerie
            </span>
          </div>
        </AppCard>
        <AppCard title="Reste à encaisser">
          <div className="kpi kpi-money">
            <strong>{formatCurrency(totals.outstanding)}</strong>
            <span className="page-subtitle">Factures non réglées ou en cours</span>
          </div>
        </AppCard>
      </div>

      <div className="toolbar" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
        <div className="filter-chips" role="group" aria-label="Statut de facturation">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.id}
              type="button"
              className={`chip${status === f.id ? ' is-active' : ''}`}
              aria-pressed={status === f.id}
              onClick={() => setStatus(f.id)}
            >
              {f.label} ({counts[f.id] || 0})
            </button>
          ))}
        </div>
        <input
          type="search"
          className="input"
          style={{ minWidth: 240, marginLeft: 'auto' }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Réf. facture, code de suivi, demandeur…"
          aria-label="Rechercher une facture"
        />
      </div>

      <AppTable
        columns={columns}
        rows={pagedRows}
        ariaLabel="Factures de la paroisse"
        emptyMessage={
          rows.length
            ? 'Aucune facture ne correspond à ce filtre.'
            : 'Aucune facture émise pour cette paroisse.'
        }
        renderCell={(row, column) => {
          switch (column.key) {
            case 'number':
              return (
                <div className="cell-stack">
                  <Link to={`/admin/factures/${row.id}`}>{row.number}</Link>
                  <span className="muted">{row.trackingCode}</span>
                </div>
              );
            case 'object':
              return (
                <div className="cell-stack">
                  <span>{row.object}</span>
                  <span className="muted cell-truncate" title={row.intention}>{row.intention}</span>
                </div>
              );
            case 'applicant':
              return (
                <div className="cell-stack">
                  <span>{row.applicant}</span>
                  <span className="muted">{row.contact}</span>
                </div>
              );
            case 'amount':
              return <strong>{formatCurrency(row.amount || 0)}</strong>;
            case 'netAmount':
              return row.netAmount != null ? formatCurrency(row.netAmount) : '—';
            case 'status':
              return <AppBadge value={row.status} label={paymentStatusLabel(row.status)} />;
            case 'paidAt':
              return row.paidAt ? formatDate(row.paidAt) : '—';
            default:
              return row[column.key];
          }
        }}
      />

      {visible.length > PAGE_SIZE ? (
        <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="muted text-sm">
            {safePage * PAGE_SIZE + 1}–{Math.min((safePage + 1) * PAGE_SIZE, visible.length)}
            {' '}sur {visible.length}
          </span>
          <div className="button-row">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={safePage <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Précédent
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={safePage >= pageCount - 1}
              onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
            >
              Suivant
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
