import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppBadge from '../../components/ui/AppBadge';
import { useTenant } from '../../hooks/useTenant';
import { formatParishTimeInUserZone } from '../../utils/formatTime';
import { useParishDemandeStats } from '../../hooks/queries/useParishDemandes';
import { useParishProgrammeQuery } from '../../hooks/queries/useParishProgramme';
import {
  useParishPastCelebrations,
  useParishUpcomingCelebrations,
} from '../../hooks/queries/useParishUpcomingCelebrations';
import { WEEK_DAY_LABELS } from '../../constants/enums';
import { isCelebrationSlotAvailable } from '../../utils/schedulingUtils';
import { ROUTES, routePath } from '../../constants/routes';
import { formatFideleName } from '../../utils/personName';
import './DashboardPage.css';

const UPCOMING_DAYS = 14;
const PAST_DAYS = 14;

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

function dayProgrammeUrl(date) {
  return routePath(ROUTES.DAILY_PROGRAMME, { date });
}

function DashboardKpiCard({ title, value, subtitle, to, action }) {
  return (
    <Link
      className="dashboard-kpi-link"
      to={to}
      aria-label={`${title} — ${action}`}
    >
      <AppCard title={title}>
        <div className="kpi dashboard-kpi">
          <strong>{value}</strong>
          <span className="page-subtitle">{subtitle}</span>
          <span className="dashboard-kpi-action">
            {action}
            <span aria-hidden="true">→</span>
          </span>
        </div>
      </AppCard>
    </Link>
  );
}

function CelebrationStatusStack({ item, includeCelebrated = false }) {
  return (
    <div className="dashboard-upcoming-statuses">
      <AppBadge value={item.statutDemande} />
      <AppBadge value={item.statutPaiement} />
      {includeCelebrated && item.celebre ? (
        <span className="badge badge-success">Célébrée</span>
      ) : null}
      {item.disponible === false ? (
        <span className="badge badge-danger">
          {item.indisponibiliteMotif || 'Indisponible'}
        </span>
      ) : null}
    </div>
  );
}

export default function DashboardPage() {
  const { activeParish } = useTenant();
  const { data, isLoading, error } = useParishDemandeStats(activeParish?.id);
  const {
    data: programme = [],
    isLoading: programmeLoading,
    error: programmeError,
  } = useParishProgrammeQuery(activeParish?.id);
  const {
    data: upcomingCelebrations = [],
    isLoading: upcomingLoading,
    error: upcomingError,
  } = useParishUpcomingCelebrations(activeParish?.id, UPCOMING_DAYS);
  const {
    data: pastCelebrations = [],
    isLoading: pastLoading,
    error: pastError,
  } = useParishPastCelebrations(activeParish?.id, PAST_DAYS);

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
    () => (programme || [])
      .map((day) => ({
        ...day,
        creneaux: Array.isArray(day.creneaux)
          ? day.creneaux.filter((slot) =>
            isCelebrationSlotAvailable(day.date, slot.heureCelebration)
          )
          : [],
      }))
      .filter((day) => day.creneaux.length > 0),
    [programme]
  );

  return (
    <div className="stack">
      <PageHeader
        title="Dashboard paroisse"
        subtitle="Indicateurs, célébrations programmées et programme des messes de votre paroisse."
      />
      {error ? <p className="text-red-600">{error.message || 'Erreur'}</p> : null}
      {isLoading ? <p className="muted">Chargement…</p> : null}

      {stats.impayeesProches > 0 ? (
        <aside className="unpaid-alert" role="alert">
          <div className="unpaid-alert-icon" aria-hidden="true">!</div>
          <div className="unpaid-alert-body">
            <strong>
              {stats.impayeesProches} célébration{stats.impayeesProches > 1 ? 's' : ''} non payée
              {stats.impayeesProches > 1 ? 's' : ''} à venir
            </strong>
            <p>
              Première célébration dans les 3 jours. Un rappel e-mail part toutes les 6 h ;
              annulation automatique 6 h avant la messe si le paiement manque.
            </p>
            <div className="unpaid-alert-actions">
              <Link className="btn btn-primary btn-sm" to={`${ROUTES.REQUESTS}?paiement=NON_PAYE`}>
                Traiter les demandes
              </Link>
              {unpaidAlert[0]?.id ? (
                <Link className="btn btn-secondary btn-sm" to={`/admin/demandes/${unpaidAlert[0].id}`}>
                  Voir la plus urgente
                </Link>
              ) : null}
            </div>
          </div>
        </aside>
      ) : null}

      <div className="card-grid dashboard-kpi-grid">
        <DashboardKpiCard
          title="Demandes totales"
          value={stats.total}
          subtitle="Enregistrées pour cette paroisse"
          to={ROUTES.REQUESTS}
          action="Voir toutes les demandes"
        />
        <DashboardKpiCard
          title="En attente"
          value={stats.pending}
          subtitle="Statut demande EN_ATTENTE"
          to={`${ROUTES.REQUESTS}?statut=EN_ATTENTE`}
          action="Traiter les demandes"
        />
        <DashboardKpiCard
          title="Validées"
          value={stats.validated}
          subtitle="Statut demande VALIDEE"
          to={`${ROUTES.REQUESTS}?statut=VALIDEE`}
          action="Voir les demandes validées"
        />
        <DashboardKpiCard
          title="Impayées proches"
          value={stats.impayeesProches}
          subtitle="Célébration ≤ 3 jours"
          to={`${ROUTES.PAYMENTS}?statut=NON_PAYE`}
          action="Voir les paiements non réglés"
        />
      </div>

      <AppCard
        title={`Célébrations programmées${upcomingCelebrations.length ? ` (${upcomingCelebrations.length})` : ''}`}
        subtitle={`${UPCOMING_DAYS} prochains jours — cliquez sur une journée pour voir et ajuster ses programmations.`}
      >
        {upcomingError ? (
          <p className="text-red-600">{upcomingError.message || 'Erreur de chargement des programmations'}</p>
        ) : null}
        {upcomingLoading ? <p className="muted">Chargement des célébrations à venir…</p> : null}
        {!upcomingLoading && !upcomingError ? (
          <div className="table-card">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Heure</th>
                  <th>Demande</th>
                  <th>Intention</th>
                  <th>Fidèle</th>
                  <th>Statuts</th>
                </tr>
              </thead>
              <tbody>
                {upcomingCelebrations.map((item) => (
                  <tr key={item.demandeDatePublicId || `${item.demandePublicId}-${item.dateCelebration}-${item.heureCelebration}`}>
                    <td data-label="Date">
                      <Link
                        className="dashboard-day-link"
                        to={dayProgrammeUrl(item.dateCelebration)}
                        title="Ouvrir les programmations de cette journée"
                      >
                        {formatProgrammeDate(item.dateCelebration)}
                        <span aria-hidden="true">→</span>
                      </Link>
                    </td>
                    <td data-label="Heure">
                      <strong>{item.heureCelebration ? formatParishTimeInUserZone(item.heureCelebration) : '—'}</strong>
                      {item.horaireLibelle ? (
                        <span className="muted" style={{ display: 'block', fontSize: '0.82em' }}>
                          {item.horaireLibelle}
                        </span>
                      ) : null}
                    </td>
                    <td data-label="Demande">
                      <Link to={`/admin/demandes/${item.demandePublicId}`}>
                        {item.codeSuivie || 'Voir'}
                      </Link>
                      <span className="muted" style={{ display: 'block', fontSize: '0.82em' }}>
                        {item.typeDemandeLibelle || '—'}
                      </span>
                    </td>
                    <td data-label="Intention" className="dashboard-upcoming-intention">
                      {item.intention || '—'}
                    </td>
                    <td data-label="Fidèle">{item.fidele || '—'}</td>
                    <td data-label="Statuts">
                      <CelebrationStatusStack item={item} />
                    </td>
                  </tr>
                ))}
                {!upcomingCelebrations.length ? (
                  <tr>
                    <td colSpan={6} className="muted" data-label="">
                      Aucune célébration enregistrée sur les {UPCOMING_DAYS} prochains jours.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        ) : null}
      </AppCard>

      <AppCard
        title={`Célébrations passées${pastCelebrations.length ? ` (${pastCelebrations.length})` : ''}`}
        subtitle={`${PAST_DAYS} derniers jours — historique des demandes dont l'heure de célébration est déjà passée.`}
      >
        {pastError ? (
          <p className="text-red-600">{pastError.message || 'Erreur de chargement de l’historique'}</p>
        ) : null}
        {pastLoading ? <p className="muted">Chargement de l’historique…</p> : null}
        {!pastLoading && !pastError ? (
          <div className="table-card">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Heure</th>
                  <th>Demande</th>
                  <th>Intention</th>
                  <th>Fidèle</th>
                  <th>Statuts</th>
                </tr>
              </thead>
              <tbody>
                {pastCelebrations.map((item) => (
                  <tr key={item.demandeDatePublicId || `${item.demandePublicId}-${item.dateCelebration}-${item.heureCelebration}`}>
                    <td data-label="Date">
                      <Link
                        className="dashboard-day-link is-history"
                        to={dayProgrammeUrl(item.dateCelebration)}
                        title="Consulter l'historique de cette journée"
                      >
                        {formatProgrammeDate(item.dateCelebration)}
                        <span aria-hidden="true">→</span>
                      </Link>
                    </td>
                    <td data-label="Heure">
                      {item.heureCelebration ? formatParishTimeInUserZone(item.heureCelebration) : '—'}
                    </td>
                    <td data-label="Demande">
                      <Link to={`/admin/demandes/${item.demandePublicId}`}>
                        {item.codeSuivie || 'Voir'}
                      </Link>
                    </td>
                    <td data-label="Intention" className="dashboard-upcoming-intention">
                      {item.intention || '—'}
                    </td>
                    <td data-label="Fidèle">{item.fidele || '—'}</td>
                    <td data-label="Statuts">
                      <CelebrationStatusStack item={item} includeCelebrated />
                    </td>
                  </tr>
                ))}
                {!pastCelebrations.length ? (
                  <tr>
                    <td colSpan={6} className="muted" data-label="">
                      Aucune célébration passée sur les {PAST_DAYS} derniers jours.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        ) : null}
      </AppCard>

      {unpaidAlert.length > 0 ? (
        <AppCard
          title="Célébrations non payées"
          subtitle="Priorité : régulariser avant la première date de célébration."
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
                  <tr key={item.id} className="unpaid-alert-row">
                    <td data-label="Code">
                      <Link to={`/admin/demandes/${item.id}`}>{item.trackingCode}</Link>
                    </td>
                    <td data-label="Demandeur">{item.applicant}</td>
                    <td data-label="Type">{item.requestType}</td>
                    <td data-label="1ère célébration">
                      <span className="unpaid-alert-date">
                        {item.firstDate ? formatProgrammeDate(item.firstDate) : '—'}
                      </span>
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
        subtitle="14 prochains jours — cliquez sur une date pour ouvrir le pilotage de la journée."
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
                      <Link className="dashboard-day-link" to={dayProgrammeUrl(day.date)}>
                        {formatProgrammeDate(day.date)}
                        <span aria-hidden="true">→</span>
                      </Link>
                      <span className="muted" style={{ display: 'block', fontSize: '0.85em' }}>
                        {WEEK_DAY_LABELS[day.jourSemaine] || day.jourLibelle || day.jourSemaine}
                      </span>
                    </td>
                    <td data-label="Créneaux">
                      <ul style={{ margin: 0, paddingLeft: 18, textAlign: 'left' }}>
                        {day.creneaux.map((c) => (
                          <li key={c.horairePublicId || `${day.date}-${c.heureCelebration}`}>
                            {formatParishTimeInUserZone(c.heureCelebration)}
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

      <AppCard title="Demandes récentes" subtitle="Les 5 derniers dépôts.">
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
