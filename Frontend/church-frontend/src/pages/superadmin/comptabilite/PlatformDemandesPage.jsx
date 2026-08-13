import React, { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { useTenant } from '../../../hooks/useTenant';
import { requestService } from '../../../services/request.service';
import { formatDate } from '../../../utils/formatDate';
import { mapDemandeToRequestRow } from '../../../utils/apiMappers';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { ROUTES, routePath } from '../../../constants/routes';

const PAGE_SIZE = 25;

const columns = [
  { key: 'parish', label: 'Paroisse' },
  { key: 'trackingCode', label: 'Code' },
  { key: 'applicant', label: 'Demandeur' },
  { key: 'requestType', label: 'Type' },
  { key: 'requestStatus', label: 'Statut' },
  { key: 'paymentStatus', label: 'Paiement' },
  { key: 'archive', label: 'Archive' },
  { key: 'createdAt', label: 'Dépôt' },
  { key: 'actions', label: 'Actions' },
];

/**
 * Audit plateforme : toutes les demandes de toutes les paroisses
 * (actives + soft-supprimées) — COMPTABLE / SUPER_ADMIN.
 */
export default function PlatformDemandesPage() {
  const navigate = useNavigate();
  const { setActiveParish } = useTenant();
  const [page, setPage] = useState(0);
  const [includeDeleted, setIncludeDeleted] = useState(true);
  const [search, setSearch] = useState('');

  const { data, isLoading, error, isFetching } = useQuery({
    queryKey: ['demandes', 'platform', page, PAGE_SIZE, includeDeleted],
    queryFn: ({ signal }) => requestService.getAllPlatform({
      page,
      size: PAGE_SIZE,
      includeDeleted,
      signal,
    }),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });

  const rows = useMemo(
    () => (data?.content || []).map((d) => ({
      ...mapDemandeToRequestRow(d),
      parish: d.paroisseNom || '—',
      paroissePublicId: d.paroissePublicId,
      paroisseNom: d.paroisseNom,
    })),
    [data]
  );

  const filteredRows = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('fr');
    if (!term) return rows;
    return rows.filter((row) => (
      row.trackingCode?.toLocaleLowerCase('fr').includes(term)
      || row.applicant?.toLocaleLowerCase('fr').includes(term)
      || row.parish?.toLocaleLowerCase('fr').includes(term)
    ));
  }, [rows, search]);

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const openDetail = (row) => {
    if (row.paroissePublicId) {
      setActiveParish({
        id: row.paroissePublicId,
        name: row.paroisseNom || row.parish,
        isSystem: false,
      });
    }
    navigate(routePath(ROUTES.REQUEST_DETAILS, { id: row.id }));
  };

  return (
    <div className="stack">
      <PageHeader
        title="Demandes — audit plateforme"
        subtitle="Traçabilité globale : toutes les paroisses, y compris les demandes archivées (soft delete)."
      />
      {error ? (
        <p className="text-red-600">{error.message || String(error)}</p>
      ) : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}

      <div className="card filters">
        <select
          className="select"
          value={includeDeleted ? 'ALL' : 'ACTIVE'}
          onChange={(e) => {
            setIncludeDeleted(e.target.value === 'ALL');
            setPage(0);
          }}
        >
          <option value="ALL">Toutes (actives + archivées)</option>
          <option value="ACTIVE">Actives uniquement</option>
        </select>
        <input
          className="input"
          placeholder="Rechercher (paroisse, code, demandeur)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <AppTable
        columns={columns}
        rows={filteredRows}
        renderCell={(row, column) => {
          if (column.key === 'trackingCode') {
            return (
              <button type="button" className="linkish" onClick={() => openDetail(row)}>
                {row.trackingCode}
              </button>
            );
          }
          if (column.key === 'requestStatus' || column.key === 'paymentStatus') {
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
          if (column.key === 'createdAt') return formatDate(row.createdAt);
          if (column.key === 'actions') {
            return (
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => openDetail(row)}>
                Voir
              </button>
            );
          }
          return row[column.key];
        }}
      />

      <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="muted" style={{ margin: 0 }}>
          {totalElements} demande{totalElements > 1 ? 's' : ''}
          {includeDeleted ? ' (toutes paroisses, actives + archivées)' : ' actives'}
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

      <p className="muted">
        Astuce : « Voir » ouvre la fiche en mode intervention sur la paroisse concernée.
        Utilisez le sélecteur de paroisse en haut pour changer de contexte, ou
        {' '}
        <Link to={ROUTES.SUBSCRIPTIONS}>Abonnements</Link>
        {' '}pour le suivi SaaS.
      </p>
    </div>
  );
}
