import React, { useEffect, useMemo, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { paymentTypeService } from '../../../services/paymentType.service';
import { mapTypePaiementToRow } from '../../../utils/apiMappers';

const MODE_OPTIONS = [
  { value: 'TMONEY', label: 'TMoney' },
  { value: 'FLOOZ', label: 'Flooz' },
  { value: 'ESPECES', label: 'Espèces' },
  { value: 'CARTE', label: 'Carte bancaire' },
];
const columns = [
  { key: 'label', label: 'Libellé' },
  { key: 'modeLabel', label: 'Mode' },
  { key: 'actions', label: 'Actions' },
];
const EMPTY_FORM = { libelle: '', mode: 'TMONEY' };
const modeLabel = (mode) => MODE_OPTIONS.find((item) => item.value === mode)?.label || mode;
const toRow = (value) => ({ ...mapTypePaiementToRow(value), modeLabel: modeLabel(value.mode) });
const sortRows = (rows) => [...rows].sort((a, b) => a.label.localeCompare(b.label, 'fr'));

export default function PaymentTypesPage() {
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
        const data = await paymentTypeService.getAll();
        if (!cancelled) setRows(sortRows((data || []).map(toRow)));
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
    return rows.filter((row) => `${row.label} ${row.modeLabel}`.toLocaleLowerCase('fr').includes(term));
  }, [rows, search]);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError(null);
    setShowForm(true);
  };

  const openEdit = (row) => {
    setEditingId(row.id);
    setForm({ libelle: row.label, mode: row.mode });
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
      const payload = { libelle: form.libelle.trim(), mode: form.mode };
      const saved = editingId
        ? await paymentTypeService.update(editingId, payload)
        : await paymentTypeService.create(payload);
      const savedRow = toRow(saved);
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
    if (!window.confirm(`Supprimer le type de paiement « ${row.label} » ?`)) return;
    try {
      setDeletingId(row.id);
      setError(null);
      await paymentTypeService.remove(row.id);
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
        title="Types de paiement"
        subtitle="Référentiel global des moyens de paiement proposés aux fidèles."
        actions={<AppButton onClick={openCreate}>Nouveau type</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <AppCard title={editingId ? 'Modifier le type de paiement' : 'Créer un type de paiement'}>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="payment-type-label">Libellé *</label>
                <AppInput id="payment-type-label" required maxLength={150} autoFocus
                  value={form.libelle}
                  onChange={(e) => setForm({ ...form, libelle: e.target.value })}
                  placeholder="Ex. Paiement TMoney" />
              </div>
              <div className="form-field">
                <label htmlFor="payment-type-mode">Mode *</label>
                <select id="payment-type-mode" className="select" required value={form.mode}
                  onChange={(e) => setForm({ ...form, mode: e.target.value })}>
                  {MODE_OPTIONS.map((mode) => (
                    <option key={mode.value} value={mode.value}>{mode.label}</option>
                  ))}
                </select>
              </div>
            </div>
            <p className="muted">Un seul type actif peut être associé à chaque mode de paiement.</p>
            <div className="button-row" style={{ marginTop: 18 }}>
              <AppButton type="submit" disabled={saving}>
                {saving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Créer le type'}
              </AppButton>
              <AppButton variant="secondary" onClick={closeForm} disabled={saving}>Annuler</AppButton>
            </div>
          </form>
        </AppCard>
      ) : null}

      <div className="card filters">
        <AppInput aria-label="Rechercher un type de paiement" value={search}
          onChange={(e) => setSearch(e.target.value)} placeholder="Rechercher par libellé ou mode" />
        <span className="muted">{filteredRows.length} type{filteredRows.length > 1 ? 's' : ''}</span>
      </div>

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && filteredRows.length === 0 ? (
        <div className="card empty-state">Aucun type de paiement ne correspond à votre recherche.</div>
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
