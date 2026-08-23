import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import { administrativeAuditService } from '../../../services/administrativeAudit.service';

const ACTION_LABELS = {
  PARISH_REGISTRATION_APPROVED: 'Inscription approuvée',
  PARISH_REGISTRATION_REJECTED: 'Inscription rejetée',
  SUBSCRIPTION_MANUALLY_ACTIVATED: 'Abonnement activé',
  SUBSCRIPTION_EXTENDED: 'Abonnement prolongé',
  SUBSCRIPTION_PENDING_CANCELLED: 'Paiement annulé',
  SUBSCRIPTION_TERMINATED: 'Abonnement résilié',
};

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export default function AdministrativeAuditPage() {
  const [events, setEvents] = useState([]);
  const [action, setAction] = useState('ALL');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function load() {
    try {
      setLoading(true);
      setError(null);
      const data = await administrativeAuditService.listRecent();
      setEvents(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de charger le journal');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const actions = useMemo(
    () => [...new Set(events.map((event) => event.action).filter(Boolean))],
    [events]
  );

  const visible = useMemo(() => {
    const needle = query.trim().toLocaleLowerCase('fr');
    return events.filter((event) => {
      if (action !== 'ALL' && event.action !== action) return false;
      if (!needle) return true;
      return [
        ACTION_LABELS[event.action],
        event.actorName,
        event.actorUsername,
        event.details,
        event.targetType,
        event.targetPublicId,
      ].filter(Boolean).some((value) =>
        String(value).toLocaleLowerCase('fr').includes(needle)
      );
    });
  }, [events, action, query]);

  return (
    <div className="stack">
      <PageHeader
        title="Journal administratif"
        subtitle="Historique récent des décisions sensibles de la plateforme."
        actions={(
          <button type="button" className="btn btn-secondary" onClick={load} disabled={loading}>
            {loading ? 'Actualisation…' : 'Actualiser'}
          </button>
        )}
      />

      <div className="alert-info" role="status">
        Ce journal est en lecture seule. Il ne contient ni mot de passe, ni document d’identité.
      </div>
      {error ? <div className="alert-error" role="alert">{error}</div> : null}

      <div className="card-grid">
        <AppCard title="Événements affichés">
          <div className="kpi">
            <strong>{visible.length}</strong>
            <span className="page-subtitle">sur les {events.length} plus récents</span>
          </div>
        </AppCard>
        <AppCard title="Dernière activité">
          <div className="kpi">
            <strong>{events.length ? formatDateTime(events[0].occurredAt) : '—'}</strong>
            <span className="page-subtitle">heure enregistrée par le serveur</span>
          </div>
        </AppCard>
      </div>

      <div className="card">
        <div className="form-grid">
          <label className="field">
            <span>Type d’action</span>
            <select className="select" value={action} onChange={(e) => setAction(e.target.value)}>
              <option value="ALL">Toutes les actions</option>
              {actions.map((value) => (
                <option key={value} value={value}>{ACTION_LABELS[value] || value}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Rechercher</span>
            <input
              className="input"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Acteur, paroisse, motif…"
            />
          </label>
        </div>
      </div>

      {loading ? <p className="muted">Chargement du journal…</p> : null}

      {!loading && visible.length === 0 ? (
        <div className="empty-state" role="status">
          <h3>Aucun événement</h3>
          <p>Aucune trace ne correspond aux critères actuels.</p>
        </div>
      ) : null}

      {visible.length > 0 ? (
        <div className="card table-card" role="region" aria-label="Journal administratif" tabIndex="0">
          <table className="app-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Action</th>
                <th>Auteur</th>
                <th>Cible</th>
                <th>Détails</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((event) => (
                <tr key={event.publicId}>
                  <td data-label="Date">{formatDateTime(event.occurredAt)}</td>
                  <td data-label="Action">
                    <strong>{ACTION_LABELS[event.action] || event.action}</strong>
                  </td>
                  <td data-label="Auteur">
                    <div className="cell-stack">
                      <span>{event.actorName || 'Système'}</span>
                      <span className="muted">{event.actorUsername || 'SYSTEM'}</span>
                    </div>
                  </td>
                  <td data-label="Cible">
                    <div className="cell-stack">
                      <span>{event.targetType}</span>
                      <span className="muted">{event.targetPublicId}</span>
                    </div>
                  </td>
                  <td data-label="Détails">{event.details || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
