import { useState } from 'react';
import { Link } from 'react-router-dom';
import AppCard from '../ui/AppCard';
import { useParishUnpaidApproaching } from '../../hooks/queries/useParishDemandes';
import { formatFideleName } from '../../utils/personName';
import { formatParishTimeInUserZone } from '../../utils/formatTime';

const PAGE_SIZE = 10;

export default function DashboardUnpaidRequests({ parishId }) {
  const [page, setPage] = useState(0);
  const { data, isLoading, isFetching, error, refetch } = useParishUnpaidApproaching(parishId, page, PAGE_SIZE);
  const rows = data?.content || [];
  const totalPages = data?.totalPages || 0;

  return (
    <AppCard
      title="Célébrations non payées"
      subtitle="Demandes classées par urgence, avant leur première célébration."
    >
      <div aria-live="polite">
        {isLoading ? <p className="muted">Chargement des demandes impayées…</p> : null}
        {error ? (
          <p role="alert">
            {error.message || 'Impossible de charger les demandes impayées.'}{' '}
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => refetch()}>
              Réessayer
            </button>
          </p>
        ) : null}
      </div>
      {!error && data ? (
        <>
          <div className="table-card" aria-busy={isFetching}>
            <table className="app-table">
              <thead>
                <tr><th>Code</th><th>Demandeur</th><th>Type</th><th>1ère célébration</th></tr>
              </thead>
              <tbody>
                {rows.map((item) => (
                  <tr key={item.publicId} className="unpaid-alert-row">
                    <td data-label="Code">
                      <Link to={`/admin/demandes/${item.publicId}`}>{item.codeSuivie}</Link>
                    </td>
                    <td data-label="Demandeur">{formatFideleName(item.prenomFidele, item.nomFidele)}</td>
                    <td data-label="Type">{item.typeDemandeLibelle || '—'}</td>
                    <td data-label="1ère célébration">
                      <span className="unpaid-alert-date">
                        {new Date(`${item.premiereCelebration.slice(0, 10)}T12:00:00`).toLocaleDateString('fr-FR')}
                        {' · '}{formatParishTimeInUserZone(item.premiereCelebration.slice(11), new Date(`${item.premiereCelebration.slice(0, 10)}T12:00:00Z`))}
                      </span>
                    </td>
                  </tr>
                ))}
                {!rows.length ? (
                  <tr><td colSpan={4} className="muted">
                    {page > 0 ? 'Cette page ne contient plus de demande.' : 'Aucune demande impayée proche de sa célébration.'}
                  </td></tr>
                ) : null}
              </tbody>
            </table>
          </div>
          <nav className="button-row dashboard-unpaid-pagination" aria-label="Pagination des demandes impayées">
            <p className="muted" aria-live="polite">
              {data.totalElements} demande{data.totalElements > 1 ? 's' : ''}
              {isFetching ? ' · actualisation…' : ''}
            </p>
            <div className="button-row">
              <button type="button" className="btn btn-secondary" disabled={page === 0 || isFetching}
                onClick={() => setPage((value) => Math.max(0, value - 1))}>
                Précédent
              </button>
              <span className="muted">Page {totalPages ? data.page + 1 : 0} / {totalPages}</span>
              <button type="button" className="btn btn-secondary" disabled={data.last || isFetching}
                onClick={() => setPage((value) => value + 1)}>
                Suivant
              </button>
              {page > 0 && !rows.length ? (
                <button type="button" className="btn btn-secondary" onClick={() => setPage(0)}>
                  Première page
                </button>
              ) : null}
            </div>
          </nav>
        </>
      ) : null}
    </AppCard>
  );
}
