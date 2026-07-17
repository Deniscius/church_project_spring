import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { parishService } from '../../../services/parish.service';
import { deaneryService } from '../../../services/deanery.service';
import { mapParoisseToTableRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'name', label: 'Paroisse' },
  { key: 'city', label: 'Doyenné' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Téléphone' },
  { key: 'active', label: 'État' },
  { key: 'actions', label: 'Actions' },
];

const EMPTY_FORM = {
  nom: '', adresse: '', email: '', telephone: '', doyennePublicId: '',
};

export default function ParishesPage() {
  const [rows, setRows] = useState([]);
  const [doyennes, setDoyennes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const [parishes, deaneries] = await Promise.all([
        parishService.getAll(), deaneryService.getAll(),
      ]);
      setRows((parishes || []).map(mapParoisseToTableRow));
      setDoyennes(deaneries || []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const closeForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError(null);
    setShowForm(true);
  };

  const openEdit = (row) => {
    setEditingId(row.id);
    setForm({
      nom: row.name || '',
      adresse: row.address || '',
      email: row.email === '—' ? '' : row.email,
      telephone: row.phone === '—' ? '' : row.phone,
      doyennePublicId: row.deaneryId || '',
    });
    setError(null);
    setShowForm(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError(null);
      const payload = {
        ...form,
        email: form.email.trim() || null,
        telephone: form.telephone.trim() || null,
      };
      if (editingId) await parishService.update(editingId, payload);
      else await parishService.create(payload);
      closeForm();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setSaving(false);
    }
  };

  const remove = async (row) => {
    if (!window.confirm(`Désactiver la paroisse « ${row.name} » ?`)) return;
    try {
      setDeletingId(row.id);
      setError(null);
      await parishService.delete(row.id);
      setRows((current) => current.filter((item) => item.id !== row.id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Désactivation impossible');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        title="Paroisses"
        subtitle="Créez les paroisses et rattachez chacune à son doyenné."
        actions={<AppButton onClick={openCreate} disabled={loading}>Nouvelle paroisse</AppButton>}
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <AppCard title={editingId ? 'Modifier la paroisse' : 'Créer une paroisse'}>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="parish-name">Nom *</label>
                <AppInput id="parish-name" required minLength={2} maxLength={100}
                  autoFocus value={form.nom}
                  onChange={(e) => setForm({ ...form, nom: e.target.value })} />
              </div>
              <div className="form-field">
                <label htmlFor="parish-deanery">Doyenné *</label>
                <select id="parish-deanery" className="select" required
                  value={form.doyennePublicId}
                  onChange={(e) => setForm({ ...form, doyennePublicId: e.target.value })}>
                  <option value="">— Sélectionner un doyenné —</option>
                  {doyennes.map((deanery) => (
                    <option key={deanery.publicId} value={deanery.publicId}>{deanery.nom}</option>
                  ))}
                </select>
              </div>
              <div className="form-field full">
                <label htmlFor="parish-address">Adresse *</label>
                <AppInput id="parish-address" required minLength={3} maxLength={200}
                  value={form.adresse}
                  onChange={(e) => setForm({ ...form, adresse: e.target.value })} />
              </div>
              <div className="form-field">
                <label htmlFor="parish-email">Email</label>
                <AppInput id="parish-email" type="email" maxLength={150} value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="form-field">
                <label htmlFor="parish-phone">Téléphone</label>
                <AppInput id="parish-phone" type="tel" maxLength={50} value={form.telephone}
                  onChange={(e) => setForm({ ...form, telephone: e.target.value })} />
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 18 }}>
              <AppButton type="submit" disabled={saving}>
                {saving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Créer la paroisse'}
              </AppButton>
              <AppButton variant="secondary" onClick={closeForm} disabled={saving}>Annuler</AppButton>
            </div>
          </form>
        </AppCard>
      ) : null}

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && rows.length === 0 ? (
        <div className="card empty-state">Aucune paroisse active.</div>
      ) : (
        <AppTable columns={columns} rows={rows} renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          if (column.key === 'actions') return (
            <div className="button-row">
              <button className="btn btn-secondary" onClick={() => openEdit(row)}>Modifier</button>
              <button className="btn btn-danger" disabled={deletingId === row.id}
                onClick={() => remove(row)}>
                {deletingId === row.id ? 'Désactivation…' : 'Désactiver'}
              </button>
            </div>
          );
          return row[column.key];
        }} />
      )}
    </div>
  );
}
