import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppLoading from '../../components/ui/AppLoading';
import {
  useHorairesByParishQuery,
  useParoissesPublicQuery,
} from '../../hooks/queries/usePublicReferentiel';
import { formatTime } from '../../utils/formatTime';
import { WEEK_DAY_LABELS } from '../../constants/enums';

function formatCreneau(slot) {
  const raw = slot.jourLibelle || slot.jourSemaine || slot.jour || '';
  const jour = WEEK_DAY_LABELS[raw] || raw;
  const heure = formatTime(slot.heureCelebration) || '';
  const libelle = slot.libelle || '';
  return [jour, heure, libelle].filter(Boolean).join(' · ');
}

function groupParishesByDoyenne(parishes) {
  const map = new Map();
  for (const p of parishes) {
    const key = (p.doyenneNom || '').trim() || 'Autres paroisses';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(p);
  }
  for (const list of map.values()) {
    list.sort((a, b) => (a.nom || '').localeCompare(b.nom || '', 'fr'));
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b, 'fr'));
}

function ParishSchedulePanel({ parish, open, onToggle }) {
  const parishId = parish.publicId;
  const { data, isLoading, isError, isFetching } = useHorairesByParishQuery(open ? parishId : null);
  const slots = Array.isArray(data) ? data : [];

  return (
    <article className={`schedule-parish${open ? ' is-open' : ''}`}>
      <button
        type="button"
        className="schedule-parish-trigger"
        aria-expanded={open}
        onClick={onToggle}
      >
        <span className="schedule-parish-name">{parish.nom}</span>
        <span className="nav-dropdown-chevron" aria-hidden="true" />
      </button>

      {open ? (
        <div className="schedule-parish-body">
          {isLoading || isFetching ? (
            <p className="muted schedule-inline-loading">Chargement des créneaux…</p>
          ) : null}
          {!isLoading && isError ? (
            <p className="muted">Impossible de charger les horaires.</p>
          ) : null}
          {!isLoading && !isError && slots.length === 0 ? (
            <p className="muted">Aucun créneau publié pour cette paroisse.</p>
          ) : null}
          {!isLoading && !isError && slots.length > 0 ? (
            <ul className="home-schedule-list">
              {slots.map((slot, idx) => (
                <li key={`${parishId}-${slot.publicId || idx}-${slot.heureCelebration}`}>
                  {formatCreneau(slot)}
                </li>
              ))}
            </ul>
          ) : null}
          <Link className="btn btn-secondary btn-sm" to="/demande">
            Faire une demande
          </Link>
        </div>
      ) : null}
    </article>
  );
}

export default function PublicSchedulesPage() {
  const { data, isLoading, isError } = useParoissesPublicQuery();
  const parishes = Array.isArray(data) ? data : [];
  const byDoyenne = useMemo(() => groupParishesByDoyenne(parishes), [parishes]);

  const [query, setQuery] = useState('');
  const [openDoyenne, setOpenDoyenne] = useState(null);
  const [openParishId, setOpenParishId] = useState(null);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return byDoyenne;
    return byDoyenne
      .map(([doyenne, list]) => [
        doyenne,
        list.filter(
          (p) =>
            (p.nom || '').toLowerCase().includes(q)
            || doyenne.toLowerCase().includes(q)
        ),
      ])
      .filter(([, list]) => list.length > 0);
  }, [byDoyenne, query]);

  return (
    <div className="stack public-page">
      <PageHeader
        title="Horaires des messes"
        subtitle="Paroisses à abonnement actif. Ouvrez un doyenné, puis une paroisse pour charger sa programmation."
      />

      <div className="schedules-toolbar">
        <label className="form-field schedules-search" htmlFor="schedules-search">
          <span className="sr-only">Rechercher un doyenné ou une paroisse</span>
          <input
            id="schedules-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher un doyenné ou une paroisse…"
            autoComplete="off"
          />
        </label>
        <Link className="btn btn-primary" to="/demande">
          Faire une demande
        </Link>
      </div>

      {isLoading ? <AppLoading message="Chargement des paroisses…" /> : null}

      {!isLoading && isError ? (
        <p className="muted">La liste des paroisses est indisponible pour le moment.</p>
      ) : null}

      {!isLoading && !isError && parishes.length === 0 ? (
        <p className="muted">Aucune paroisse active pour le moment.</p>
      ) : null}

      {!isLoading && !isError && parishes.length > 0 && filtered.length === 0 ? (
        <p className="muted">Aucun résultat pour « {query.trim()} ».</p>
      ) : null}

      {!isLoading && filtered.length > 0 ? (
        <div className="schedules-doyennes">
          {filtered.map(([doyenne, list]) => {
            const isOpen = openDoyenne === doyenne;
            return (
              <section key={doyenne} className={`schedules-doyenne-acc${isOpen ? ' is-open' : ''}`}>
                <button
                  type="button"
                  className="schedules-doyenne-trigger"
                  aria-expanded={isOpen}
                  onClick={() => {
                    setOpenDoyenne((cur) => (cur === doyenne ? null : doyenne));
                    setOpenParishId(null);
                  }}
                >
                  <span>
                    <span className="schedules-doyenne-label">{doyenne}</span>
                    <span className="muted schedules-doyenne-count">
                      {list.length} paroisse{list.length > 1 ? 's' : ''}
                    </span>
                  </span>
                  <span className="nav-dropdown-chevron" aria-hidden="true" />
                </button>

                {isOpen ? (
                  <div className="schedules-parish-list">
                    {list.map((parish) => (
                      <ParishSchedulePanel
                        key={parish.publicId}
                        parish={parish}
                        open={openParishId === parish.publicId}
                        onToggle={() =>
                          setOpenParishId((cur) =>
                            cur === parish.publicId ? null : parish.publicId
                          )
                        }
                      />
                    ))}
                  </div>
                ) : null}
              </section>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
