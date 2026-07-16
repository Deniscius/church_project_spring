import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { localityService } from '../../../services/locality.service';
import { mapLocaliteToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'city', label: 'Ville' },
  { key: 'district', label: 'Quartier' },
  { key: 'actions', label: 'Actions' },
];

const EMPTY_FORM = { ville: '', quartier: '' };
const sortRows = (rows) => [...rows].sort((a, b) => a.label.localeCompare(b.label, 'fr'));

export default function LocalitiesPage() {
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
        const data = await localityService.getAll();
        if (!cancelled) setRows(sortRows((data || []).map(mapLocaliteToRow)));
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
    setForm({ ville: row.city, quartier: row.district });
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
      const payload = { ville: form.ville.trim(), quartier: form.quartier.trim() };
      const saved = editingId
        ? await localityService.update(editingId, payload)
        : await localityService.create(payload);
      const savedRow = mapLocaliteToRow(saved);
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
    if (!window.confirm(`Supprimer la localité « ${row.label} » ?`)) return;
    try {
      setDeletingId(row.id);
      setError(null);
      await localityService.remove(row.id);
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
        title="Localités"
        subtitle="Référentiel géographique utilisé lors de la création des paroisses."
        actions={<AppButton onClick={openCreate}>Nouvelle localité</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <AppCard title={editingId ? 'Modifier la localité' : 'Créer une localité'}>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="locality-city">Ville *</label>
                <AppInput id="locality-city" required minLength={2} maxLength={150}
                  autoFocus value={form.ville}
                  onChange={(e) => setForm({ ...form, ville: e.target.value })}
                  placeholder="Ex. Lomé" />
              </div>
              <div className="form-field">
                <label htmlFor="locality-district">Quartier *</label>
                <AppInput id="locality-district" required minLength={2} maxLength={200}
                  value={form.quartier}
                  onChange={(e) => setForm({ ...form, quartier: e.target.value })}
                  placeholder="Ex. Tokoin" />
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 18 }}>
              <AppButton type="submit" disabled={saving}>
                {saving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Créer la localité'}
              </AppButton>
              <AppButton variant="secondary" onClick={closeForm} disabled={saving}>Annuler</AppButton>
            </div>
          </form>
        </AppCard>
      ) : null}

      <div className="card filters">
        <AppInput aria-label="Rechercher une localité" value={search}
          onChange={(e) => setSearch(e.target.value)} placeholder="Rechercher une ville ou un quartier" />
        <span className="muted">{filteredRows.length} localité{filteredRows.length > 1 ? 's' : ''}</span>
      </div>

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && filteredRows.length === 0 ? (
        <div className="card empty-state">Aucune localité ne correspond à votre recherche.</div>
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
