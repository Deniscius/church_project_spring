import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import { useTenant } from '../../hooks/useTenant';
import { formatTime } from '../../utils/formatTime';
import { useParishDemandeStats } from '../../hooks/queries/useParishDemandes';
import { useParishProgrammeQuery } from '../../hooks/queries/useParishProgramme';
import { WEEK_DAY_LABELS } from '../../constants/enums';
import { formatFideleName } from '../../utils/personName';

function formatProgrammeDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
    });
  } catch {
    return iso;
  }
}

export default function DashboardPage() {
  const { activeParish } = useTenant();
  const { data, isLoading, error } = useParishDemandeStats(activeParish?.id);
  const {
    data: programme = [],
    isLoading: programmeLoading,
    error: programmeError,
  } = useParishProgrammeQuery(activeParish?.id);

  const stats = {
    total: data?.total ?? 0,
    pending: data?.enAttente ?? 0,
    validated: data?.validees ?? 0,
    impayeesProches: data?.impayeesProchesCelebration ?? 0,
  };
  const unpaidAlert = (data?.impayeesProches || []).map((d) => ({
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: formatFideleName(d.prenomFidele, d.nomFidele),
    requestType: d.typeDemandeLibelle || '—',
    firstDate: Array.isArray(d.datesCelebration) && d.datesCelebration.length
      ? d.datesCelebration[0]
      : null,
    montant: d.montant,
  }));
  const recent = (data?.recentes || []).map((d) => ({
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: formatFideleName(d.prenomFidele, d.nomFidele),
    requestType: d.typeDemandeLibelle || '—',
    requestStatus: d.statutDemande,
  }));

  const programmeRows = useMemo(
    () =>
      (programme || []).filter(
        (day) => Array.isArray(day.creneaux) && day.creneaux.length > 0
      ),
    [programme]
  );

  return (
    <div className="stack">
      <PageHeader
        title="Dashboard paroisse"
        subtitle="Indicateurs agrégés et programme des messes (messe unique respectée)."
      />
      {error ? <p className="text-red-600">{error.message || 'Erreur'}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}

      {stats.impayeesProches > 0 ? (
        <div className="alert-warning" role="alert">
          <strong>{stats.impayeesProches} demande{stats.impayeesProches > 1 ? 's' : ''} non payée{stats.impayeesProches > 1 ? 's' : ''}</strong>
          {' '}avec célébration dans les 3 jours — le fidèle reçoit un rappel e-mail toutes les 6 h.
          {' '}
          <Link to="/admin/demandes">Voir les demandes</Link>
        </div>
      ) : null}

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
        <AppCard title="Impayées proches">
          <div className="kpi">
            <strong>{stats.impayeesProches}</strong>
            <span className="page-subtitle">Célébration ≤ 3 jours</span>
          </div>
        </AppCard>
      </div>

      {unpaidAlert.length > 0 ? (
        <AppCard
          title="Alertes — paiements manquants"
          subtitle="Demandes non réglées dont la première célébration approche."
        >
          <div className="table-card">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Demandeur</th>
                  <th>Type</th>
                  <th>1ère célébration</th>
                </tr>
              </thead>
              <tbody>
                {unpaidAlert.map((item) => (
                  <tr key={item.id}>
                    <td data-label="Code">
                      <Link to={`/admin/demandes/${item.id}`}>{item.trackingCode}</Link>
                    </td>
                    <td data-label="Demandeur">{item.applicant}</td>
                    <td data-label="Type">{item.requestType}</td>
                    <td data-label="1ère célébration">
                      {item.firstDate ? formatProgrammeDate(item.firstDate) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AppCard>
      ) : null}

      <AppCard
        title="Programme des messes"
        subtitle="14 prochains jours — une messe unique masque les autres créneaux du jour."
      >
        {programmeError ? (
          <p className="text-red-600">{programmeError.message || 'Erreur programme'}</p>
        ) : null}
        {programmeLoading ? <p className="muted">Chargement du programme…</p> : null}
        {!programmeLoading && !programmeError ? (
          <div className="table-card">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Créneaux</th>
                  <th>Note</th>
                </tr>
              </thead>
              <tbody>
                {programmeRows.map((day) => (
                  <tr key={day.date}>
                    <td data-label="Date">
                      {formatProgrammeDate(day.date)}
                      <span className="muted" style={{ display: 'block', fontSize: '0.85em' }}>
                        {WEEK_DAY_LABELS[day.jourSemaine] || day.jourLibelle || day.jourSemaine}
                      </span>
                    </td>
                    <td data-label="Créneaux">
                      <ul style={{ margin: 0, paddingLeft: 18, textAlign: 'left' }}>
                        {day.creneaux.map((c) => (
                          <li key={c.horairePublicId || `${day.date}-${c.heureCelebration}`}>
                            {formatTime(c.heureCelebration)}
                            {c.libelle ? ` · ${c.libelle}` : ''}
                            {c.dateSpecifique ? ' · ponctuelle' : ''}
                          </li>
                        ))}
                      </ul>
                    </td>
                    <td data-label="Note">
                      {day.messeUnique ? (
                        <span className="text-red-600">Messe unique</span>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
                {!programmeRows.length ? (
                  <tr>
                    <td colSpan={3} className="muted" data-label="">
                      Aucun créneau sur les 14 prochains jours.{' '}
                      <Link to="/admin/horaires">Configurer les horaires</Link>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        ) : null}
      </AppCard>

      <AppCard title="Demandes récentes" subtitle="Les 5 dernières dépôts.">
        <div className="table-card">
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
                  <td data-label="Code">
                    <Link to={`/admin/demandes/${item.id}`}>{item.trackingCode}</Link>
                  </td>
                  <td data-label="Demandeur">{item.applicant}</td>
                  <td data-label="Type">{item.requestType}</td>
                  <td data-label="Statut">{item.requestStatus}</td>
                </tr>
              ))}
              {!recent.length && !isLoading ? (
                <tr>
                  <td colSpan={4} className="muted" data-label="">
                    Aucune demande pour cette paroisse.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </AppCard>
    </div>
  );
}
