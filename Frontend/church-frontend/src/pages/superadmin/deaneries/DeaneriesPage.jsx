import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { deaneryService } from '../../../services/deanery.service';
import { mapDoyenneToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'name', label: 'Doyenné' },
  { key: 'description', label: 'Description' },
  { key: 'actions', label: 'Actions' },
];

const EMPTY_FORM = { nom: '', description: '' };
const sortRows = (rows) => [...rows].sort((a, b) => a.label.localeCompare(b.label, 'fr'));

export default function DeaneriesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

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
    if (!term) return rows;
    return rows.filter((row) => row.label.toLocaleLowerCase('fr').includes(term));
  }, [rows, search]);

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

  const remove = async (row) => {
    if (!window.confirm(`Supprimer le doyenné « ${row.label} » ?`)) return;
    try {
      setDeletingId(row.id);
      setError(null);
      await deaneryService.remove(row.id);
      setRows((current) => current.filter((item) => item.id !== row.id));
      if (editingId === row.id) closeForm();
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
        subtitle="Organisez les paroisses par doyenné. Un doyenné peut regrouper plusieurs paroisses."
        actions={<AppButton onClick={openCreate}>Nouveau doyenné</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <AppCard title={editingId ? 'Modifier le doyenné' : 'Créer un doyenné'}>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="deanery-name">Nom *</label>
                <AppInput id="deanery-name" required minLength={2} maxLength={400}
                  autoFocus value={form.nom}
                  onChange={(e) => setForm({ ...form, nom: e.target.value })}
                  placeholder="Ex. Doyenné de Lomé-Centre" />
              </div>
              <div className="form-field">
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
        </AppCard>
      ) : null}

      <div className="card filters">
        <AppInput aria-label="Rechercher un doyenné" value={search}
          onChange={(e) => setSearch(e.target.value)} placeholder="Rechercher par nom ou description" />
        <span className="muted">{filteredRows.length} doyenné{filteredRows.length > 1 ? 's' : ''}</span>
      </div>

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && filteredRows.length === 0 ? (
        <div className="card empty-state">Aucun doyenné ne correspond à votre recherche.</div>
      ) : (
        <AppTable columns={columns} rows={filteredRows} renderCell={(row, column) => {
          if (column.key === 'actions') return (
            <div className="button-row">
              <button className="btn btn-secondary" onClick={() => openEdit(row)}>Modifier</button>
              <button className="btn btn-danger" disabled={deletingId === row.id}
                onClick={() => remove(row)}>
                {deletingId === row.id ? 'Suppression…' : 'Supprimer'}
              </button>
            </div>
          );
          return row[column.key];
        }} />
      )}
    </div>
  );
}
