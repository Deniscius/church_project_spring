import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import { useTenant } from '../../../hooks/useTenant';
import { celebrationService } from '../../../services/celebration.service';
import { formatDate } from '../../../utils/formatDate';
import { formatTime } from '../../../utils/formatTime';
import { formatFideleName } from '../../../utils/personName';

const columns = [
  { key: 'intention', label: 'Intention de messe' },
  { key: 'demandeur', label: 'Demandeur' },
  { key: 'contact', label: 'Contact' },
  { key: 'type', label: 'Type / forfait' },
  { key: 'progression', label: 'Progression' },
  { key: 'code', label: 'Réf.' },
];

const NO_TIME_KEY = 'SANS_HEURE';

function todayIso() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function groupKey(group) {
  return formatTime(group.heure) || NO_TIME_KEY;
}

function groupTitle(group) {
  const heure = formatTime(group.heure);
  return group.libelleMesse || (heure ? `Messe de ${heure}` : 'Messe — heure non précisée');
}

function mapIntentionRow(item) {
  const demandeur = formatFideleName(item.demandeurPrenom, item.demandeurNom);
  const contact = [item.demandeurTelephone, item.demandeurEmail].filter(Boolean).join(' · ') || '—';
  let type = item.typeDemandeLibelle || '';
  if (item.forfaitNom) type = type ? `${type} — ${item.forfaitNom}` : item.forfaitNom;
  if (item.nombreCelebration > 1 && item.dureeLabel) type += ` (${item.dureeLabel})`;
  if (item.natureForfait === 'SPECIALE') type += ' · spéciale';

  return {
    id: item.demandeDatePublicId,
    intention: item.intention || '—',
    demandeur,
    contact,
    type: type || '—',
    progression: item.progressionLabel || '—',
    code: item.codeSuivie || '—',
  };
}

export default function CelebrationSheetPage() {
  const { activeParish } = useTenant();
  const [date, setDate] = useState(todayIso);
  const [inclureNonPayees, setInclureNonPayees] = useState(false);
  const [groups, setGroups] = useState([]);
  const [selectedHeures, setSelectedHeures] = useState([]);
  const [loading, setLoading] = useState(false);
  const [printing, setPrinting] = useState(false);
  const [error, setError] = useState(null);

  // La journée est toujours chargée entière : les créneaux disponibles restent
  // visibles même quand une seule messe est sélectionnée.
  useEffect(() => {
    if (!activeParish?.id || !date) {
      setGroups([]);
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await celebrationService.listByParishAndDate(activeParish.id, {
          date,
          inclureNonPayees,
        });
        if (!cancelled) setGroups(Array.isArray(data) ? data : []);
      } catch (e) {
        if (!cancelled) {
          setGroups([]);
          setError(e instanceof Error ? e.message : 'Impossible de charger les intentions');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeParish?.id, date, inclureNonPayees]);

  useEffect(() => {
    setSelectedHeures([]);
  }, [date, inclureNonPayees, activeParish?.id]);

  const visibleGroups = useMemo(() => {
    if (!selectedHeures.length) return groups;
    return groups.filter((group) => selectedHeures.includes(groupKey(group)));
  }, [groups, selectedHeures]);

  const countIntentions = (list) =>
    list.reduce((sum, group) => sum + (group.nombreIntentions ?? group.intentions?.length ?? 0), 0);

  const totalIntentions = useMemo(() => countIntentions(groups), [groups]);
  const visibleIntentions = useMemo(() => countIntentions(visibleGroups), [visibleGroups]);

  const toggleHeure = (key) => {
    setSelectedHeures((current) =>
      current.includes(key) ? current.filter((h) => h !== key) : [...current, key]
    );
  };

  const printPdf = async () => {
    if (!activeParish?.id || !date) return;
    try {
      setPrinting(true);
      setError(null);
      const blob = await celebrationService.downloadFeuillePdf(activeParish.id, {
        date,
        inclureNonPayees,
        heures: selectedHeures.filter((key) => key !== NO_TIME_KEY),
      });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      const suffix = selectedHeures.length ? `-${selectedHeures.join('-').replace(/:/g, 'h')}` : '';
      anchor.download = `feuille-intentions-${date}${suffix}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impression impossible');
    } finally {
      setPrinting(false);
    }
  };

  const printLabel = selectedHeures.length
    ? `Imprimer ${visibleGroups.length} messe${visibleGroups.length > 1 ? 's' : ''}`
    : 'Imprimer la journée';

  return (
    <div className="stack">
      <PageHeader
        title="Feuille d'intentions"
        subtitle="Intentions regroupées par heure de messe pour la date de célébration choisie."
        actions={(
          <button
            className="btn btn-primary"
            type="button"
            onClick={printPdf}
            disabled={printing || !activeParish?.id || !visibleGroups.length}
          >
            {printing ? 'Préparation…' : printLabel}
          </button>
        )}
      />

      <div className="toolbar filters-row" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'end' }}>
        <label className="field" style={{ display: 'grid', gap: '0.35rem' }}>
          <span className="muted">Date de célébration</span>
          <input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <label className="field" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', paddingBottom: '0.35rem' }}>
          <input
            type="checkbox"
            checked={inclureNonPayees}
            onChange={(e) => setInclureNonPayees(e.target.checked)}
          />
          <span>Inclure les non payées</span>
        </label>
        <p className="muted" style={{ margin: 0, paddingBottom: '0.4rem' }}>
          {formatDate(date)} · {visibleGroups.length} messe{visibleGroups.length > 1 ? 's' : ''} ·{' '}
          {visibleIntentions} intention{visibleIntentions > 1 ? 's' : ''}
          {selectedHeures.length ? ` (sur ${groups.length} · ${totalIntentions})` : ''}
        </p>
      </div>

      {groups.length ? (
        <div className="filter-chips" role="group" aria-label="Heures de célébration">
          <button
            type="button"
            className={`chip${selectedHeures.length === 0 ? ' is-active' : ''}`}
            aria-pressed={selectedHeures.length === 0}
            onClick={() => setSelectedHeures([])}
          >
            Toutes les messes ({totalIntentions})
          </button>
          {groups.map((group) => {
            const key = groupKey(group);
            const heure = formatTime(group.heure);
            const count = group.nombreIntentions ?? group.intentions?.length ?? 0;
            return (
              <button
                key={key}
                type="button"
                className={`chip${selectedHeures.includes(key) ? ' is-active' : ''}`}
                aria-pressed={selectedHeures.includes(key)}
                onClick={() => toggleHeure(key)}
              >
                {heure || 'Heure non précisée'} ({count})
              </button>
            );
          })}
        </div>
      ) : null}

      {error ? <p className="text-red-600">{error}</p> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      {!loading && !groups.length ? (
        <div className="empty-state" role="status">
          <h3>Aucune intention</h3>
          <p>Aucune intention validée pour cette date de célébration.</p>
        </div>
      ) : null}

      {!loading && groups.length && !visibleGroups.length ? (
        <div className="empty-state" role="status">
          <h3>Aucune messe sélectionnée</h3>
          <p>Choisissez au moins une heure de célébration ci-dessus.</p>
        </div>
      ) : null}

      {visibleGroups.map((group) => {
        const title = groupTitle(group);
        const rows = (group.intentions || []).map(mapIntentionRow);
        return (
          <section key={groupKey(group)} className="messe-group card">
            <header className="messe-group-header">
              <div>
                <p className="messe-group-kicker">Heure de célébration</p>
                <h2 className="messe-group-title">{title}</h2>
              </div>
              <span className="messe-group-count">
                {group.nombreIntentions ?? rows.length} intention{(group.nombreIntentions ?? rows.length) > 1 ? 's' : ''}
              </span>
            </header>
            <AppTable
              columns={columns}
              rows={rows}
              emptyMessage="Aucune intention pour cette messe."
              ariaLabel={`Intentions ${title}`}
            />
          </section>
        );
      })}
    </div>
  );
}
