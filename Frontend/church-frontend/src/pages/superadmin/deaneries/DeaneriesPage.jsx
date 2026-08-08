import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import { deaneryService } from '../../../services/deanery.service';
import { mapDoyenneToRow } from '../../../utils/apiMappers';
import { setDoyenneFilter } from '../../../utils/sensitiveNav';

const columns = [
  { key: 'rang', label: 'Rang' },
  { key: 'name', label: 'Doyenné' },
  { key: 'description', label: 'Description' },
  { key: 'actions', label: 'Actions' },
];

const EMPTY_FORM = { nom: '', description: '', rang: '' };
const sortRows = (rows) => [...rows].sort((a, b) => {
  const ra = a.rang ?? 999;
  const rb = b.rang ?? 999;
  if (ra !== rb) return ra - rb;
  return a.label.localeCompare(b.label, 'fr');
});

export default function DeaneriesPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [rangFilter, setRangFilter] = useState('ALL');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [pendingDelete, setPendingDelete] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await deaneryService.getAll();
        if (!cancelled) setRows(sortRows((data || []).map(mapDoyenneToRow)));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Chargement impossible');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const filteredRows = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('fr');
    return rows.filter((row) => {
      if (rangFilter !== 'ALL' && String(row.rang ?? '') !== rangFilter) return false;
      if (!term) return true;
      return row.label.toLocaleLowerCase('fr').includes(term)
        || String(row.description || '').toLocaleLowerCase('fr').includes(term);
    });
  }, [rows, search, rangFilter]);

  const rangOptions = useMemo(() => {
    const values = [...new Set(rows.map((r) => r.rang).filter((r) => r != null))].sort((a, b) => a - b);
    return values;
  }, [rows]);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError(null);
    setShowForm(true);
  };

  const openEdit = (row) => {
    setEditingId(row.id);
    setForm({
      nom: row.name === '—' ? '' : row.name,
      description: row.description === '—' ? '' : row.description,
      rang: row.rang != null ? String(row.rang) : '',
    });
    setError(null);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError(null);
      const payload = {
        nom: form.nom.trim(),
        description: form.description.trim() || null,
        rang: form.rang.trim() ? Number(form.rang) : null,
      };
      const saved = editingId
        ? await deaneryService.update(editingId, payload)
        : await deaneryService.create(payload);
      const savedRow = mapDoyenneToRow(saved);
      setRows((current) => sortRows(
        editingId
          ? current.map((row) => row.id === editingId ? savedRow : row)
          : [...current, savedRow]
      ));
      closeForm();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!pendingDelete) return;
    try {
      setDeletingId(pendingDelete.id);
      setError(null);
      await deaneryService.remove(pendingDelete.id);
      setRows((current) => current.filter((item) => item.id !== pendingDelete.id));
      if (editingId === pendingDelete.id) closeForm();
      setPendingDelete(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Suppression impossible');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        title="Doyennés"
        subtitle="Ordre officiel du diocèse. Chaque doyenné regroupe les paroisses de son territoire."
        actions={<AppButton onClick={openCreate}>Nouveau doyenné</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <div
          className="card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="deanery-form-title"
        >
          <h2 id="deanery-form-title" className="section-title">
            {editingId ? 'Modifier le doyenné' : 'Créer un doyenné'}
          </h2>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="deanery-name">Nom *</label>
                <AppInput id="deanery-name" required minLength={2} maxLength={400}
                  autoFocus value={form.nom}
                  onChange={(e) => setForm({ ...form, nom: e.target.value })}
                  placeholder="Ex. Lomé-Centre" />
              </div>
              <div className="form-field">
                <label htmlFor="deanery-rang">Rang</label>
                <AppInput id="deanery-rang" type="number" min={1}
                  value={form.rang}
                  onChange={(e) => setForm({ ...form, rang: e.target.value })}
                  placeholder="À défaut : placé en dernier" />
              </div>
              <div className="form-field full">
                <label htmlFor="deanery-description">Description</label>
                <AppInput id="deanery-description" maxLength={500}
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Zone pastorale ou précision facultative" />
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 18 }}>
              <AppButton type="submit" disabled={saving}>
                {saving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Créer le doyenné'}
              </AppButton>
              <AppButton variant="secondary" onClick={closeForm} disabled={saving}>Annuler</AppButton>
            </div>
          </form>
        </div>
      ) : null}

      <div className="card filters">
        <AppInput aria-label="Rechercher un doyenné" value={search}
          onChange={(e) => setSearch(e.target.value)} placeholder="Rechercher par nom ou description" />
        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span>Rang</span>
          <select
            className="select"
            value={rangFilter}
            onChange={(e) => setRangFilter(e.target.value)}
            aria-label="Filtrer par rang"
          >
            <option value="ALL">Tous</option>
            {rangOptions.map((rang) => (
              <option key={rang} value={String(rang)}>{rang}</option>
            ))}
          </select>
        </label>
        <span className="muted">{filteredRows.length} doyenné{filteredRows.length > 1 ? 's' : ''}</span>
      </div>

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && filteredRows.length === 0 ? (
        <div className="card empty-state">Aucun doyenné ne correspond à votre recherche.</div>
      ) : (
        <AppTable columns={columns} rows={filteredRows} renderCell={(row, column) => {
          if (column.key === 'rang') return row.rang ?? '—';
          if (column.key === 'actions') return (
            <div className="button-row">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setDoyenneFilter(row.id);
                  navigate('/admin/paroisses', { state: { doyenneId: row.id, openCreate: true } });
                }}
              >
                Ajouter une paroisse
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setDoyenneFilter(row.id);
                  navigate('/admin/paroisses', { state: { doyenneId: row.id } });
                }}
              >
                Voir les paroisses
              </button>
              <button className="btn btn-secondary" onClick={() => openEdit(row)}>Modifier</button>
              <button className="btn btn-danger" disabled={deletingId === row.id}
                onClick={() => setPendingDelete(row)}>
                {deletingId === row.id ? 'Suppression…' : 'Supprimer'}
              </button>
            </div>
          );
          return row[column.key];
        }} />
      )}

      <AppDialog
        open={Boolean(pendingDelete)}
        title="Supprimer le doyenné"
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        danger
        busy={Boolean(deletingId)}
        onCancel={() => setPendingDelete(null)}
        onConfirm={remove}
      >
        {pendingDelete ? (
          <p style={{ margin: 0 }}>
            Confirmer la suppression du doyenné « {pendingDelete.label} » ?
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
