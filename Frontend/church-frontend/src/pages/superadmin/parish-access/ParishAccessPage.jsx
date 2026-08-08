import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppBadge from '../../../components/ui/AppBadge';
import AppInput from '../../../components/ui/AppInput';
import { parishAccessService } from '../../../services/parishAccess.service';
import { mapParoisseAccessToRow } from '../../../utils/apiMappers';
import { formatRole } from '../../../utils/roleMapper';

const ROLE_PAROISSE_LABELS = {
  ADMIN: 'Administrateur',
  GESTIONNAIRE: 'Gestionnaire',
  SECRETAIRE: 'Secrétaire',
  CONSULTATION: 'Consultation',
};

const FILTERS = [
  { id: 'ALL', label: 'Toutes', match: () => true },
  { id: 'SANS_CONTACT', label: 'Sans contact', match: (r) => !r.userEmail && !r.userTelephone },
  { id: 'SUSPENDUES', label: 'Paroisses suspendues', match: (r) => !r.parishActive },
  { id: 'INACTIFS', label: 'Accès révoqués', match: (r) => !r.isActive },
];

function formatDate(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
}

export default function ParishAccessPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await parishAccessService.getAll();
        if (!cancelled) setRows((data || []).map(mapParoisseAccessToRow));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur de chargement');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const counts = useMemo(() => {
    const base = {};
    FILTERS.forEach((f) => {
      base[f.id] = rows.filter(f.match).length;
    });
    return base;
  }, [rows]);

  const visible = useMemo(() => {
    const active = FILTERS.find((f) => f.id === filter) || FILTERS[0];
    const needle = search.trim().toLowerCase();
    return rows.filter((r) => {
      if (!active.match(r)) return false;
      if (!needle) return true;
      return [r.user, r.username, r.userEmail, r.userTelephone, r.parish, r.deanery]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(needle));
    });
  }, [rows, filter, search]);

  // Un accès se lit toujours dans le contexte de sa paroisse : on regroupe
  // plutôt que d'aligner des lignes utilisateur sans rattachement visible.
  const groups = useMemo(() => {
    const byParish = new Map();
    visible.forEach((row) => {
      const key = row.parishId || row.parish;
      if (!byParish.has(key)) {
        byParish.set(key, {
          key,
          nom: row.parish,
          deanery: row.deanery,
          email: row.parishEmail,
          telephone: row.parishTelephone,
          active: row.parishActive,
          expiresAt: row.subscriptionExpiresAt,
          accesses: [],
        });
      }
      byParish.get(key).accesses.push(row);
    });
    return [...byParish.values()].sort((a, b) => (a.nom || '').localeCompare(b.nom || ''));
  }, [visible]);

  return (
    <div className="stack">
      <PageHeader
        title="Accès paroisses"
        subtitle="Qui accède à quelle paroisse, avec les coordonnées permettant de joindre chaque responsable."
      />

      {error ? <div className="alert-error" role="alert">{error}</div> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      <div className="filter-chips" role="group" aria-label="Filtres des accès">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            className={`chip${filter === f.id ? ' is-active' : ''}`}
            aria-pressed={filter === f.id}
            onClick={() => setFilter(f.id)}
          >
            {f.label} ({counts[f.id]})
          </button>
        ))}
      </div>

      <div className="form-field" style={{ maxWidth: 420 }}>
        <label htmlFor="access-search">Rechercher</label>
        <AppInput
          id="access-search"
          type="search"
          placeholder="Nom, identifiant, e-mail, paroisse, doyenné…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {!loading && groups.length === 0 ? (
        <div className="empty-state" role="status">
          <h3>Aucun accès trouvé</h3>
          <p>
            {rows.length === 0
              ? 'Aucun rattachement utilisateur ↔ paroisse n’existe encore.'
              : 'Aucun accès ne correspond à cette recherche ou à ce filtre.'}
          </p>
        </div>
      ) : null}

      {groups.map((group) => {
        const expiry = formatDate(group.expiresAt);
        return (
          <section className="card" key={group.key}>
            <header className="access-group-head">
              <div>
                <h2 className="access-group-title">{group.nom}</h2>
                <p className="muted" style={{ margin: '2px 0 0' }}>
                  {[group.deanery, [group.email, group.telephone].filter(Boolean).join(' · ')]
                    .filter(Boolean)
                    .join(' — ') || 'Aucune coordonnée renseignée'}
                </p>
              </div>
              <div className="access-group-meta">
                <AppBadge
                  value={group.active ? 'ACTIVE' : 'INACTIVE'}
                  label={group.active ? 'Paroisse active' : 'Accès suspendu'}
                />
                {expiry ? <span className="muted">Abonnement jusqu’au {expiry}</span> : null}
              </div>
            </header>

            <div className="table-wrap">
              <table className="app-table">
                <thead>
                  <tr>
                    <th>Utilisateur</th>
                    <th>Coordonnées</th>
                    <th>Rôle dans la paroisse</th>
                    <th>Rôle applicatif</th>
                    <th>État</th>
                  </tr>
                </thead>
                <tbody>
                  {group.accesses.map((row) => (
                    <tr key={row.id}>
                      <td data-label="Utilisateur">
                        <div className="cell-stack">
                          <strong>{row.user}</strong>
                          <span className="muted">{row.username || '—'}</span>
                        </div>
                      </td>
                      <td data-label="Coordonnées">
                        <div className="cell-stack">
                          {row.userEmail ? (
                            <a href={`mailto:${row.userEmail}`}>{row.userEmail}</a>
                          ) : (
                            <span className="muted">Pas d’e-mail</span>
                          )}
                          {row.userTelephone ? (
                            <a href={`tel:${row.userTelephone}`}>{row.userTelephone}</a>
                          ) : (
                            <span className="muted">Pas de téléphone</span>
                          )}
                        </div>
                      </td>
                      <td data-label="Rôle dans la paroisse">
                        {ROLE_PAROISSE_LABELS[row.role] || row.role || '—'}
                      </td>
                      <td data-label="Rôle applicatif">{formatRole(row.userRole)}</td>
                      <td data-label="État">
                        <div className="cell-stack">
                          <AppBadge
                            value={row.active}
                            label={row.isActive ? 'Accès actif' : 'Accès révoqué'}
                          />
                          {!row.userActive ? <span className="muted">Compte désactivé</span> : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        );
      })}
    </div>
  );
}
