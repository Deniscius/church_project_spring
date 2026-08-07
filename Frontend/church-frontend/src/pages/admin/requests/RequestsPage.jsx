import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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

const activeColumns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut demande' },
  { key: 'validationStatus', label: 'Validation' },
  { key: 'paymentStatus', label: 'Statut paiement' },
  { key: 'createdAt', label: 'Date de dépôt' },
  { key: 'celebrationDates', label: 'Célébration' },
  { key: 'actions', label: 'Actions' },
];

const deletedColumns = [
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut' },
  { key: 'deletedAt', label: 'Supprimée le' },
  { key: 'deletedByNom', label: 'Par' },
  { key: 'actions', label: 'Actions' },
];

export default function RequestsPage() {
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [paymentFilter, setPaymentFilter] = useState('');
  const [search, setSearch] = useState('');
  const [showDeleted, setShowDeleted] = useState(false);
  const [deletedRows, setDeletedRows] = useState([]);
  const [deletedLoading, setDeletedLoading] = useState(false);
  const [deletedError, setDeletedError] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const invalidate = useInvalidateParishDemandes();

  const { data, isLoading, error, isFetching } = useParishDemandesPage(
    showDeleted ? null : activeParish?.id,
    page,
    PAGE_SIZE
  );

  useEffect(() => {
    if (!showDeleted || !activeParish?.id) {
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
            deletedAt: d.deletedAt,
            deletedByNom: d.deletedByNom || '—',
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
  }, [showDeleted, activeParish?.id]);

  const rows = useMemo(
    () => (data?.content || []).map(mapDemandeToRequestRow),
    [data]
  );

  const filteredRows = useMemo(() => {
    const source = showDeleted ? deletedRows : rows;
    return source.filter((row) => {
      const term = search.trim().toLocaleLowerCase('fr');
      const matchesSearch = !term
        || row.trackingCode?.toLocaleLowerCase('fr').includes(term)
        || row.applicant?.toLocaleLowerCase('fr').includes(term);
      if (showDeleted) return matchesSearch;
      return matchesSearch
        && (!statusFilter || row.requestStatus === statusFilter)
        && (!paymentFilter || row.paymentStatus === paymentFilter);
    });
  }, [rows, deletedRows, showDeleted, search, statusFilter, paymentFilter]);

  const confirmRemove = async () => {
    if (!pendingDelete) return;
    try {
      setDeleting(true);
      await requestService.remove(pendingDelete.id);
      setPendingDelete(null);
      invalidate(activeParish?.id);
      if (showDeleted) {
        setDeletedRows((current) => current.filter((r) => r.id !== pendingDelete.id));
      }
    } finally {
      setDeleting(false);
    }
  };

  const totalPages = data?.totalPages ?? 0;
  const totalElements = showDeleted ? filteredRows.length : (data?.totalElements ?? 0);
  const columns = showDeleted ? deletedColumns : activeColumns;

  return (
    <div className="stack">
      <PageHeader
        title="Demandes"
        subtitle="Liste paginée des demandes de la paroisse active. La suppression est un archivage (soft delete) avec trace."
      />
      {error || deletedError ? (
        <p className="text-red-600">
          {(error && (error.message || String(error))) || deletedError}
        </p>
      ) : null}
      {(isLoading && !showDeleted) || (deletedLoading && showDeleted) ? <p className="muted">Chargement…</p> : null}
      <div className="card filters">
        <select
          className="select"
          value={showDeleted ? 'DELETED' : 'ACTIVE'}
          onChange={(e) => {
            const deleted = e.target.value === 'DELETED';
            setShowDeleted(deleted);
            setPage(0);
          }}
        >
          <option value="ACTIVE">Demandes actives</option>
          <option value="DELETED">Demandes supprimées (trace)</option>
        </select>
        {!showDeleted ? (
          <>
            <select className="select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">Tous les statuts</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="VALIDEE">Validée</option>
              <option value="REJETEE">Rejetée</option>
              <option value="ANNULEE">Annulée</option>
            </select>
            <select className="select" value={paymentFilter} onChange={(e) => setPaymentFilter(e.target.value)}>
              <option value="">Tous les paiements</option>
              <option value="NON_PAYE">Non payé</option>
              <option value="PAYE">Payé</option>
              <option value="ECHOUE">Échoué</option>
            </select>
          </>
        ) : null}
        <input className="input" placeholder="Rechercher (code ou demandeur)" value={search}
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
                {!showDeleted && has(PERMISSIONS.DEMAND_DELETE) ? (
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
      {!showDeleted ? (
        <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <p className="muted" style={{ margin: 0 }}>
            {totalElements} demande{totalElements > 1 ? 's' : ''}
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
        <p className="muted">{totalElements} demande{totalElements > 1 ? 's' : ''} archivée{totalElements > 1 ? 's' : ''}</p>
      )}

      <AppDialog
        open={Boolean(pendingDelete)}
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
            mais reste consultable dans « Demandes supprimées » avec votre identité et l’heure.
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
