import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import BankNameField from '../../../components/ui/BankNameField';
import { validateRibTogo } from '../../../utils/ribTogo';
import { parishService } from '../../../services/parish.service';
import { deaneryService } from '../../../services/deanery.service';
import { mapParoisseToTableRow, mapParoisseToTenant } from '../../../utils/apiMappers';
import { tenantStatusLabel } from '../../../utils/statusMapper';
import { formatDate } from '../../../utils/formatDate';
import { getDoyenneFilter, setDoyenneFilter } from '../../../utils/sensitiveNav';
import { useTenant } from '../../../hooks/useTenant';
import { useToast } from '../../../contexts/toast.context';

const columns = [
  { key: 'name', label: 'Paroisse' },
  { key: 'city', label: 'Doyenné' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Téléphone' },
  { key: 'active', label: 'État' },
  { key: 'subscription', label: 'Abonnement' },
  { key: 'actions', label: 'Actions' },
];

const EMPTY_FORM = {
  nom: '',
  adresse: '',
  email: '',
  telephone: '',
  doyennePublicId: '',
  nomBanque: '',
  titulaireCompte: '',
  ibanOrRib: '',
};

// « Annuaire » et « Abonnées » sont les deux extrémités du tunnel commercial :
// les filtres suivent cet ordre pour se lire comme un entonnoir.
const FILTERS = [
  ['ALL', 'Toutes'],
  ['PROSPECT', 'Annuaire'],
  ['EN_ATTENTE_PAIEMENT', 'En attente de paiement'],
  ['ACTIVE', 'Abonnées'],
  ['EN_TOLERANCE', 'Échéance dépassée'],
  ['SUSPENDUE', 'Suspendues'],
];

export default function ParishesPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { setActiveParish } = useTenant();
  const [rows, setRows] = useState([]);
  const [doyennes, setDoyennes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [filter, setFilter] = useState('ALL');
  const [doyenneFilter, setDoyenneFilterState] = useState(() => getDoyenneFilter());
  const [search, setSearch] = useState('');

  const applyDoyenneFilter = (id) => {
    setDoyenneFilter(id || '');
    setDoyenneFilterState(id || '');
  };

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const [parishes, deaneries] = await Promise.all([
        parishService.getAll(),
        deaneryService.getAll(),
      ]);
      setRows((parishes || []).map(mapParoisseToTableRow));
      setDoyennes(deaneries || []);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Arrivée depuis Doyennés via location.state (pas d’UUID dans l’URL).
  useEffect(() => {
    const payload = location.state;
    if (!payload || typeof payload !== 'object') return;
    if (payload.doyenneId) {
      applyDoyenneFilter(payload.doyenneId);
    }
    if (payload.openCreate) {
      setEditingId(null);
      setForm({ ...EMPTY_FORM, doyennePublicId: payload.doyenneId || getDoyenneFilter() || '' });
      setFilter('PROSPECT');
      setError(null);
      setShowForm(true);
    }
    navigate(location.pathname, { replace: true, state: null });
  }, [location.state, location.pathname, navigate]);

  // Compat anciennes URL ?doyenne=
  useEffect(() => {
    const qs = new URLSearchParams(window.location.search);
    const legacy = qs.get('doyenne');
    const openNew = qs.get('nouvelle') === '1';
    if (!legacy && !openNew) return;
    if (legacy) applyDoyenneFilter(legacy);
    if (openNew) {
      setEditingId(null);
      setForm({ ...EMPTY_FORM, doyennePublicId: legacy || getDoyenneFilter() || '' });
      setFilter('PROSPECT');
      setShowForm(true);
    }
    window.history.replaceState({}, '', window.location.pathname);
  }, []);

  const visibleRows = useMemo(() => {
    let list = rows;
    if (doyenneFilter) list = list.filter((r) => r.deaneryId === doyenneFilter);
    if (filter !== 'ALL') list = list.filter((r) => r.active === filter);
    const q = search.trim().toLowerCase();
    if (q) {
      list = list.filter((r) => {
        const hay = [r.name, r.city, r.email, r.phone, r.address]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return hay.includes(q);
      });
    }
    return list;
  }, [rows, filter, doyenneFilter, search]);

  const stats = useMemo(() => {
    const scope = doyenneFilter ? rows.filter((r) => r.deaneryId === doyenneFilter) : rows;
    return {
      total: scope.length,
      prospects: scope.filter((r) => r.active === 'PROSPECT').length,
      pending: scope.filter((r) => r.active === 'EN_ATTENTE_PAIEMENT').length,
      active: scope.filter((r) => r.active === 'ACTIVE').length,
      aRelancer: scope.filter((r) => r.active === 'EN_TOLERANCE' || r.active === 'SUSPENDUE').length,
    };
  }, [rows, doyenneFilter]);

  const closeForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const openCreate = () => {
    setEditingId(null);
    setForm({ ...EMPTY_FORM, doyennePublicId: doyenneFilter || '' });
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
      nomBanque: row.nomBanque || '',
      titulaireCompte: row.titulaireCompte || '',
      ibanOrRib: row.ibanOrRib || '',
    });
    setError(null);
    setShowForm(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError(null);
      const banque = form.nomBanque.trim() || null;
      let rib = form.ibanOrRib.trim() || null;
      if (rib) {
        const ribCheck = validateRibTogo(rib, banque);
        if (!ribCheck.valid) {
          setError(ribCheck.message);
          setSaving(false);
          return;
        }
        rib = ribCheck.normalized;
      }
      const payload = {
        ...form,
        email: form.email.trim() || null,
        telephone: form.telephone.trim() || null,
        nomBanque: banque,
        titulaireCompte: form.titulaireCompte.trim() || null,
        ibanOrRib: rib,
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

  const remove = async () => {
    if (!pendingDelete) return;
    try {
      setDeletingId(pendingDelete.id);
      setError(null);
      await parishService.delete(pendingDelete.id);
      setRows((current) => current.filter((item) => item.id !== pendingDelete.id));
      setPendingDelete(null);
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
        subtitle="Annuaire des tenants. Intervenez uniquement en cas de souci — le quotidien reste à l’équipe locale."
        actions={
          <div className="button-row">
            <Link className="btn btn-secondary" to="/admin/inscriptions-paroisse" style={{ textDecoration: 'none' }}>
              Inscriptions
            </Link>
            <AppButton onClick={openCreate} disabled={loading}>Ajouter à l’annuaire</AppButton>
          </div>
        }
      />

      <div className="card-grid">
        <AppCard title="Annuaire"><strong style={{ fontSize: '1.5rem' }}>{stats.prospects}</strong></AppCard>
        <AppCard title="En attente de paiement"><strong style={{ fontSize: '1.5rem' }}>{stats.pending}</strong></AppCard>
        <AppCard title="Abonnées"><strong style={{ fontSize: '1.5rem' }}>{stats.active}</strong></AppCard>
        <AppCard title="À relancer"><strong style={{ fontSize: '1.5rem' }}>{stats.aRelancer}</strong></AppCard>
      </div>

      <div className="button-row" style={{ flexWrap: 'wrap', alignItems: 'center', gap: 12 }}>
        <label className="muted parish-search-field" style={{ display: 'flex', alignItems: 'center', gap: 8, flex: '1 1 220px' }}>
          <span className="sr-only">Rechercher un tenant</span>
          <AppInput
            id="parish-search"
            type="search"
            placeholder="Rechercher (nom, doyenné, email, tél.)…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Rechercher un tenant"
          />
        </label>
        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span>Doyenné</span>
          <select
            className="select"
            value={doyenneFilter}
            onChange={(e) => applyDoyenneFilter(e.target.value)}
            aria-label="Filtrer par doyenné"
          >
            <option value="">Tous les doyennés</option>
            {[...doyennes]
              .sort((a, b) => (a.rang ?? 999) - (b.rang ?? 999) || String(a.nom).localeCompare(b.nom, 'fr'))
              .map((d) => (
                <option key={d.publicId} value={d.publicId}>
                  {d.rang != null ? `${d.rang}. ` : ''}{d.nom}
                </option>
              ))}
          </select>
        </label>
        {FILTERS.map(([value, label]) => (
          <button
            key={value}
            type="button"
            className={`btn ${filter === value ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>

      {doyenneFilter ? (
        <p className="muted">
          Filtré sur le doyenné{' '}
          <strong>{doyennes.find((d) => d.publicId === doyenneFilter)?.nom || 'sélectionné'}</strong>
          {' · '}
          <button
            type="button"
            className="link-button"
            onClick={() => applyDoyenneFilter('')}
          >
            Réinitialiser
          </button>
        </p>
      ) : null}

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}

      {showForm ? (
        <AppCard title={editingId ? 'Modifier la paroisse' : 'Ajouter une paroisse à l’annuaire'}>
          <p className="muted" style={{ marginTop: 0 }}>
            La fiche apparaît dans le doyenné comme prospect. Elle n’ouvre l’accès plateforme
            qu’après inscription et paiement de l’abonnement.
          </p>
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
                <label htmlFor="parish-email">E-mail professionnel</label>
                <AppInput
                  id="parish-email"
                  type="email"
                  maxLength={150}
                  value={form.email}
                  readOnly
                  disabled
                />
                <small className="muted">
                  {editingId
                    ? 'Adresse générée automatiquement — non modifiable.'
                    : 'Sera générée automatiquement à l’enregistrement (contact@… ).'}
                </small>
              </div>
              <div className="form-field">
                <label htmlFor="parish-phone">Téléphone</label>
                <AppInput id="parish-phone" type="tel" maxLength={50} value={form.telephone}
                  onChange={(e) => setForm({ ...form, telephone: e.target.value })} />
              </div>
              <div className="form-field">
                <label htmlFor="parish-bank">Banque</label>
                <BankNameField
                  id="parish-bank"
                  value={form.nomBanque}
                  onChange={(nomBanque) => setForm({ ...form, nomBanque })}
                />
              </div>
              <div className="form-field">
                <label htmlFor="parish-holder">Titulaire</label>
                <AppInput id="parish-holder" maxLength={150} value={form.titulaireCompte}
                  onChange={(e) => setForm({ ...form, titulaireCompte: e.target.value })} />
              </div>
              <div className="form-field full">
                <label htmlFor="parish-rib">RIB / IBAN</label>
                <AppInput
                  id="parish-rib"
                  maxLength={80}
                  spellCheck={false}
                  autoComplete="off"
                  placeholder="Ex. TG53 TG00 9060 4310 3465 0040 0070"
                  value={form.ibanOrRib}
                  onChange={(e) => setForm({ ...form, ibanOrRib: e.target.value })}
                />
                {form.ibanOrRib.trim() ? (() => {
                  const live = validateRibTogo(form.ibanOrRib, form.nomBanque);
                  return (
                    <small
                      role="status"
                      style={{ color: live.valid ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}
                    >
                      {live.message}
                    </small>
                  );
                })() : (
                  <small className="muted">
                    IBAN Togo (28 car.) ou RIB domestique (24 car.). Vérifié avec la banque choisie.
                  </small>
                )}
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 18 }}>
              <AppButton type="submit" disabled={saving}>
                {saving ? 'Enregistrement…' : editingId ? 'Enregistrer les modifications' : 'Ajouter à l’annuaire'}
              </AppButton>
              <AppButton variant="secondary" onClick={closeForm} disabled={saving}>Annuler</AppButton>
            </div>
          </form>
        </AppCard>
      ) : null}

      {loading ? <p className="muted">Chargement…</p> : null}
      {!loading && visibleRows.length === 0 ? (
        <div className="card empty-state">
          {search.trim()
            ? `Aucun tenant ne correspond à « ${search.trim()} ».`
            : 'Aucune paroisse pour ce filtre.'}
        </div>
      ) : (
        <AppTable columns={columns} rows={visibleRows} renderCell={(row, column) => {
          if (column.key === 'active') {
            return <AppBadge value={row.active} label={tenantStatusLabel(row.active)} />;
          }
          if (column.key === 'subscription') {
            return row.subscriptionExpiresAt
              ? formatDate(row.subscriptionExpiresAt)
              : <span className="muted">—</span>;
          }
          if (column.key === 'actions') {
            return (
              <div className="button-row">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={async () => {
                    try {
                      const full = await parishService.getById(row.id);
                      const tenant = mapParoisseToTenant(full);
                      if (!tenant?.id) {
                        toast.error('Paroisse introuvable.');
                        return;
                      }
                      setActiveParish(tenant);
                      toast.success(`Intervention : ${tenant.name}`);
                      navigate('/admin/dashboard');
                    } catch (e) {
                      toast.error(e instanceof Error ? e.message : 'Impossible d’intervenir sur la paroisse');
                    }
                  }}
                >
                  Intervenir
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => openEdit(row)}>Modifier</button>
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={deletingId === row.id}
                  onClick={() => setPendingDelete(row)}
                >
                  {deletingId === row.id ? 'Désactivation…' : 'Désactiver'}
                </button>
              </div>
            );
          }
          return row[column.key];
        }} />
      )}

      <AppDialog
        open={Boolean(pendingDelete)}
        title="Désactiver la paroisse"
        confirmLabel="Désactiver"
        cancelLabel="Annuler"
        danger
        busy={Boolean(deletingId)}
        onCancel={() => setPendingDelete(null)}
        onConfirm={remove}
      >
        {pendingDelete ? (
          <p style={{ margin: 0 }}>
            Désactiver la paroisse « {pendingDelete.name} » ?
          </p>
        ) : null}
      </AppDialog>
    </div>
  );
}
