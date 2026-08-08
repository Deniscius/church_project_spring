import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import PdfPreviewModal from '../../../components/ui/PdfPreviewModal';
import { useTenant } from '../../../hooks/useTenant';
import { celebrationService } from '../../../services/celebration.service';
import { requestService } from '../../../services/request.service';
import { formatDate } from '../../../utils/formatDate';
import { formatParishTimeInUserZone, formatTime } from '../../../utils/formatTime';
import { formatFideleName } from '../../../utils/personName';
import { useToast } from '../../../contexts/toast.context';

const columns = [
  { key: 'intention', label: 'Intention' },
  { key: 'demandeur', label: 'Demandeur' },
  { key: 'progression', label: 'Progression' },
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
  const heure = formatParishTimeInUserZone(group.heure);
  return group.libelleMesse || (heure ? `Messe de ${heure}` : 'Messe — heure non précisée');
}

function mapIntentionRow(item) {
  return {
    id: item.demandeDatePublicId,
    demandePublicId: item.demandePublicId,
    codeSuivie: item.codeSuivie,
    intention: item.intention || '—',
    demandeur: formatFideleName(item.demandeurPrenom, item.demandeurNom),
    progression: item.progressionLabel || '—',
    rawIntention: item.intention || '',
  };
}

export default function CelebrationSheetPage() {
  const { activeParish } = useTenant();
  const toast = useToast();
  const [date, setDate] = useState(todayIso);
  const [inclureNonPayees, setInclureNonPayees] = useState(false);
  const [groups, setGroups] = useState([]);
  const [selectedHeures, setSelectedHeures] = useState([]);
  const [loading, setLoading] = useState(false);
  const [printing, setPrinting] = useState(false);
  const [error, setError] = useState(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewError, setPreviewError] = useState(null);
  const [previewName, setPreviewName] = useState('feuille-intentions.pdf');

  const [editRow, setEditRow] = useState(null);
  const [editText, setEditText] = useState('');
  const [editBusy, setEditBusy] = useState(false);

  const reload = async () => {
    if (!activeParish?.id || !date) {
      setGroups([]);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const data = await celebrationService.listByParishAndDate(activeParish.id, {
        date,
        inclureNonPayees,
      });
      setGroups(Array.isArray(data) ? data : []);
    } catch (e) {
      setGroups([]);
      setError(e instanceof Error ? e.message : 'Impossible de charger les intentions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!activeParish?.id || !date) {
        setGroups([]);
        return;
      }
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

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

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

  const openEdit = (row) => {
    if (!row?.demandePublicId) return;
    setEditRow(row);
    setEditText(row.rawIntention || '');
  };

  const saveIntention = async () => {
    if (!editRow?.demandePublicId) return;
    const text = editText.trim();
    if (text.length < 3) {
      toast.error('L’intention doit contenir au moins 3 caractères.');
      return;
    }
    if (text.length > 500) {
      toast.error('L’intention ne peut pas dépasser 500 caractères.');
      return;
    }
    setEditBusy(true);
    try {
      await requestService.updateIntention(editRow.demandePublicId, text);
      toast.success('Intention mise à jour.');
      setEditRow(null);
      await reload();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Modification impossible');
    } finally {
      setEditBusy(false);
    }
  };

  const openPdfPreview = async () => {
    if (!activeParish?.id || !date) return;
    try {
      setPrinting(true);
      setError(null);
      setPreviewError(null);
      setPreviewOpen(true);
      const blob = await celebrationService.downloadFeuillePdf(activeParish.id, {
        date,
        inclureNonPayees,
        heures: selectedHeures.filter((key) => key !== NO_TIME_KEY),
      });
      const suffix = selectedHeures.length ? `-${selectedHeures.join('-').replace(/:/g, 'h')}` : '';
      const name = `feuille-intentions-${date}${suffix}.pdf`;
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl(URL.createObjectURL(blob));
      setPreviewName(name);
    } catch (e) {
      setPreviewError(e instanceof Error ? e.message : 'Aperçu impossible');
      setPreviewUrl(null);
      setError(e instanceof Error ? e.message : 'Impression impossible');
    } finally {
      setPrinting(false);
    }
  };

  const downloadPreview = () => {
    if (!previewUrl) return;
    const anchor = document.createElement('a');
    anchor.href = previewUrl;
    anchor.download = previewName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  };

  const printLabel = selectedHeures.length
    ? `Aperçu PDF (${visibleGroups.length} messe${visibleGroups.length > 1 ? 's' : ''})`
    : 'Aperçu PDF de la journée';

  return (
    <div className="stack">
      <PageHeader
        title="Feuille d'intentions"
        subtitle="Vue légère pour le célébrant : cliquez une ligne pour modifier l’intention."
        actions={(
          <AppButton
            variant="primary"
            type="button"
            onClick={openPdfPreview}
            disabled={printing || !activeParish?.id || !visibleGroups.length}
            loading={printing}
          >
            {printing ? 'Préparation…' : printLabel}
          </AppButton>
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
            const heure = formatParishTimeInUserZone(group.heure);
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
              onRowClick={openEdit}
            />
          </section>
        );
      })}

      <AppDialog
        open={Boolean(editRow)}
        title="Modifier l’intention"
        confirmLabel="Enregistrer"
        cancelLabel="Annuler"
        busy={editBusy}
        size="lg"
        promptLabel="Texte de l’intention"
        promptValue={editText}
        onPromptChange={setEditText}
        promptPlaceholder="Ex. Pour le repos de l’âme de…"
        promptRows={4}
        onCancel={() => {
          if (!editBusy) setEditRow(null);
        }}
        onConfirm={saveIntention}
      >
        {editRow ? (
          <p className="muted" style={{ marginTop: 0 }}>
            {editRow.demandeur}
            {editRow.codeSuivie ? ` · ${editRow.codeSuivie}` : ''}
          </p>
        ) : null}
      </AppDialog>

      <PdfPreviewModal
        open={previewOpen}
        title="Aperçu de la feuille d’intentions"
        blobUrl={previewUrl}
        fileName={previewName}
        loading={printing}
        error={previewError}
        onClose={() => setPreviewOpen(false)}
        onDownload={downloadPreview}
      />
    </div>
  );
}
