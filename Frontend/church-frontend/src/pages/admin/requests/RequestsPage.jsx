import React, { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { useTenant } from '../../../hooks/useTenant';
import { requestService } from '../../../services/request.service';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToRequestRow } from '../../../utils/apiMappers';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import {
  useInvalidateParishDemandes,
  useParishDemandesPage,
} from '../../../hooks/queries/useParishDemandes';

const PAGE_SIZE = 20;
const REQUEST_STATUS_FILTERS = new Set(['EN_ATTENTE', 'VALIDEE', 'REJETEE', 'ANNULEE']);
const PAYMENT_STATUS_FILTERS = new Set(['NON_PAYE', 'PAYE', 'ECHOUE']);

const columns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut demande' },
  { key: 'validationStatus', label: 'Validation' },
  { key: 'paymentStatus', label: 'Statut paiement' },
  { key: 'archive', label: 'Archive' },
  { key: 'createdAt', label: 'Date de dépôt' },
  { key: 'celebrationDates', label: 'Célébration' },
  { key: 'actions', label: 'Actions' },
];

function normalizeSearchText(value) {
  return String(value ?? '').trim().toLocaleLowerCase('fr');
}

function digitsOnly(value) {
  return String(value ?? '').replace(/\D/g, '');
}

export default function RequestsPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [searchParams, setSearchParams] = useSearchParams();
  const canAudit = has(PERMISSIONS.DEMAND_AUDIT);
  const canDelete = has(PERMISSIONS.DEMAND_DELETE);

  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [listScope, setListScope] = useState(canAudit ? 'ALL' : 'ACTIVE');
  const [deletedRows, setDeletedRows] = useState([]);
  const [deletedLoading, setDeletedLoading] = useState(false);
  const [deletedError, setDeletedError] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const invalidate = useInvalidateParishDemandes();

  const statusParam = searchParams.get('statut') || '';
  const paymentParam = searchParams.get('paiement') || '';
  const statusFilter = REQUEST_STATUS_FILTERS.has(statusParam) ? statusParam : '';
  const paymentFilter = PAYMENT_STATUS_FILTERS.has(paymentParam) ? paymentParam : '';

  const updateFilterParam = (key, value) => {
    const nextParams = new URLSearchParams(searchParams);
    if (value) nextParams.set(key, value);
    else nextParams.delete(key);
    setSearchParams(nextParams, { replace: true });
    setPage(0);
  };

  const includeDeleted = canAudit && listScope === 'ALL';
  const showDeletedOnly = canAudit && listScope === 'DELETED';

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(search.trim());
    }, 300);
    return () => window.clearTimeout(timer);
  }, [search]);

  const { data, isLoading, error, isFetching } = useParishDemandesPage(
    showDeletedOnly ? null : activeParish?.id,
    page,
    PAGE_SIZE,
    includeDeleted,
    debouncedSearch
  );

  useEffect(() => {
    if (!showDeletedOnly || !activeParish?.id) {
      setDeletedRows([]);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        setDeletedLoading(true);
        setDeletedError(null);
        const raw = await requestService.getDeletedByParish(activeParish.id);
        if (!cancelled) {
          setDeletedRows((raw || []).map((d) => ({
            ...mapDemandeToRequestRow(d),
            statusDel: true,
          })));
        }
      } catch (e) {
        if (!cancelled) setDeletedError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setDeletedLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [showDeletedOnly, activeParish?.id]);

  const rows = useMemo(
    () => (data?.content || []).map(mapDemandeToRequestRow),
    [data]
  );

  const filteredRows = useMemo(() => {
    const source = showDeletedOnly ? deletedRows : rows;
    const term = normalizeSearchText(search);
    const phoneTerm = digitsOnly(search);

    return source.filter((row) => {
      const rawPhone = row._raw?.telFidele || '';
      const matchesPhone = Boolean(term) && (
        normalizeSearchText(rawPhone).includes(term)
        || (phoneTerm.length >= 4 && digitsOnly(rawPhone).includes(phoneTerm))
      );
      const matchesSearch = !term
        || normalizeSearchText(row.trackingCode).includes(term)
        || normalizeSearchText(row.applicant).includes(term)
        || matchesPhone;
      if (showDeletedOnly) return matchesSearch;
      return matchesSearch
        && (!statusFilter || row.requestStatus === statusFilter)
        && (!paymentFilter || row.paymentStatus === paymentFilter);
    });
  }, [rows, deletedRows, showDeletedOnly, search, statusFilter, paymentFilter]);

  const confirmRemove = async () => {
    if (!canDelete || !pendingDelete) return;
    try {
      setDeleting(true);
      await requestService.remove(pendingDelete.id);
      setPendingDelete(null);
      invalidate(activeParish?.id);
      if (showDeletedOnly) {
        setDeletedRows((current) => current.filter((r) => r.id !== pendingDelete.id));
      }
    } finally {
      setDeleting(false);
    }
  };

  const totalPages = data?.totalPages ?? 0;
  const totalElements = showDeletedOnly ? filteredRows.length : (data?.totalElements ?? 0);

  return (
    <div className="stack">
      <PageHeader
        title="Demandes"
        subtitle={
          canAudit
            ? 'Vue audit : demandes actives et archivées avec traçabilité.'
            : 'Liste paginée des demandes de la paroisse active.'
        }
      />
      {error || deletedError ? (
        <p className="text-red-600">
          {(error && (error.message || String(error))) || deletedError}
        </p>
      ) : null}
      {(isLoading && !showDeletedOnly) || (deletedLoading && showDeletedOnly) ? (
        <p className="muted">Chargement…</p>
      ) : null}
      <div className="card filters">
        <select
          className="select"
          value={canAudit ? listScope : 'ACTIVE'}
          onChange={(e) => {
            setListScope(e.target.value);
            setPage(0);
          }}
        >
          {canAudit ? <option value="ALL">Toutes (actives + archivées)</option> : null}
          <option value="ACTIVE">Demandes actives</option>
          {canAudit ? <option value="DELETED">Demandes supprimées (trace)</option> : null}
        </select>
        {!showDeletedOnly ? (
          <>
            <select
              className="select"
              value={statusFilter}
              onChange={(e) => updateFilterParam('statut', e.target.value)}
            >
              <option value="">Tous les statuts</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="VALIDEE">Validée</option>
              <option value="REJETEE">Rejetée</option>
              <option value="ANNULEE">Annulée</option>
            </select>
            <select
              className="select"
              value={paymentFilter}
              onChange={(e) => updateFilterParam('paiement', e.target.value)}
            >
              <option value="">Tous les paiements</option>
              <option value="NON_PAYE">Non payé</option>
              <option value="PAYE">Payé</option>
              <option value="ECHOUE">Échoué</option>
            </select>
          </>
        ) : null}
        <input
          className="input"
          placeholder="Rechercher (code, demandeur ou téléphone)"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
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
          if (column.key === 'archive') {
            if (row.statusDel) {
              return (
                <span title={row.deletedByNom ? `Par ${row.deletedByNom}` : undefined}>
                  <AppBadge value="SUPPRIMEE" />
                  {row.deletedAt ? (
                    <small className="muted" style={{ display: 'block' }}>{formatDate(row.deletedAt)}</small>
                  ) : null}
                </span>
              );
            }
            return <span className="muted">—</span>;
          }
          if (column.key === 'createdAt' || column.key === 'deletedAt') {
            return formatDate(row[column.key]);
          }
          if (column.key === 'celebrationDates') {
            return row.celebrationDates || '—';
          }
          if (column.key === 'actions') {
            return (
              <div className="button-row">
                <Link className="btn btn-secondary" to={`/admin/demandes/${row.id}`}>Voir</Link>
                {!row.statusDel && canDelete ? (
                  <button type="button" className="btn btn-danger" onClick={() => setPendingDelete(row)}>
                    Supprimer
                  </button>
                ) : null}
              </div>
            );
          }
          return row[column.key];
        }}
      />
      {!showDeletedOnly ? (
        <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <p className="muted" style={{ margin: 0 }}>
            {totalElements} demande{totalElements > 1 ? 's' : ''}
            {includeDeleted ? ' (actives + archivées)' : ''}
            {isFetching && !isLoading ? ' · actualisation…' : ''}
          </p>
          <div className="button-row">
            <button
              type="button"
              className="btn btn-secondary"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Précédent
            </button>
            <span className="muted">
              Page {totalPages === 0 ? 0 : page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={page >= totalPages - 1 || totalPages === 0}
              onClick={() => setPage((p) => p + 1)}
            >
              Suivant
            </button>
          </div>
        </div>
      ) : (
        <p className="muted">
          {totalElements} demande{totalElements > 1 ? 's' : ''} archivée{totalElements > 1 ? 's' : ''}
        </p>
      )}

      <AppDialog
        open={Boolean(pendingDelete) && canDelete}
        title="Archiver la demande"
        confirmLabel="Archiver"
        cancelLabel="Annuler"
        danger
        busy={deleting}
        onCancel={() => setPendingDelete(null)}
        onConfirm={confirmRemove}
      >
        {pendingDelete ? (
          <p style={{ margin: 0 }}>
            Soft delete de « {pendingDelete.trackingCode} » : la demande disparaît des listes actives
            mais reste consultable par les comptes disposant de la permission d’audit, avec votre identité et l’heure.
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
