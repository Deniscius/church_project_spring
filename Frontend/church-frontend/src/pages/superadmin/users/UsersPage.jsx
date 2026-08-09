import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import AppDialog from '../../../components/ui/AppDialog';
import { userService } from '../../../services/user.service';
import { parishService } from '../../../services/parish.service';
import { mapUserToRow } from '../../../utils/apiMappers';
import { useAuth } from '../../../hooks/useAuth';

const columns = [
  { key: 'firstName', label: 'Prénom' },
  { key: 'lastName', label: 'Nom' },
  { key: 'username', label: 'Username' },
  { key: 'contact', label: 'Contact' },
  { key: 'role', label: 'Rôle' },
  { key: 'active', label: 'État' },
];

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const currentUserId = currentUser?.id || currentUser?.publicId;
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pendingDeactivate, setPendingDeactivate] = useState(null);
  const [deactivating, setDeactivating] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingIsGlobal, setEditingIsGlobal] = useState(null);
  const [parishes, setParishes] = useState([]);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    username: '',
    email: '',
    telephone: '',
    password: '',
    role: 'SECRETAIRE',
    isActive: true,
    isGlobal: false,
    paroisseId: null,
    roleParoisse: 'SECRETAIRE',
  });

  const isEditingSelf = Boolean(editingId) && editingId === currentUserId;
  useEffect(() => {
    loadUsers();
    loadParishes();
  }, []);

  const loadParishes = async () => {
    try {
      const data = await parishService.getAll();
      setParishes(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error('Erreur lors du chargement des paroisses:', e);
    }
  };

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getAll();
      setRows((data || []).map(mapUserToRow));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (name === 'role') {
      setFormData(prev => ({
        ...prev,
        role: value,
        isGlobal: value === 'SUPER_ADMIN' || value === 'COMPTABLE',
      }));
      return;
    }
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError(null);

      const payload = {
        nom: formData.nom,
        prenom: formData.prenom,
        username: formData.username,
        email: formData.email,
        telephone: formData.telephone,
        role: formData.role,
        isActive: isEditingSelf ? true : formData.isActive,
        isGlobal: formData.isGlobal,
      };

      if (editingId) {
        // Identifiant / mot de passe non modifiables par le SUPER_ADMIN
        payload.password = '';
        await userService.update(editingId, payload);
      } else {
        const createPayload = {
          ...payload,
          password: formData.password,
          paroisses: formData.isGlobal
            ? []
            : [{
                paroisseId: formData.paroisseId,
                roleParoisse: formData.roleParoisse,
              }],
        };

        await userService.create(createPayload);
      }

      await loadUsers();
      handleCancel();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (userId) => {
    try {
      const user = rows.find(row => row.id === userId);
      if (user) {
        setFormData({
          nom: user.lastName || '',
          prenom: user.firstName || '',
          username: user.username || '',
          email: user.email || '',
          telephone: user.telephone || '',
          password: '',
          role: user.role || 'SECRETAIRE',
          isActive: user.isActive,
          isGlobal: user.isGlobal,
          paroisseId: null,
          roleParoisse: 'SECRETAIRE',
        });
        setEditingId(userId);
        setEditingIsGlobal(Boolean(user.isGlobal));
        setShowForm(true);
      }
    } catch {
      setError('Erreur lors de la récupération');
    }
  };

  const handleDelete = async () => {
    if (!pendingDeactivate) return;
    if (pendingDeactivate === currentUserId) {
      setError('Vous ne pouvez pas désactiver votre propre compte');
      setPendingDeactivate(null);
      return;
    }
    try {
      setDeactivating(true);
      await userService.delete(pendingDeactivate);
      setPendingDeactivate(null);
      await loadUsers();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setDeactivating(false);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setEditingIsGlobal(null);
    setFormData({
      nom: '',
      prenom: '',
      username: '',
      email: '',
      telephone: '',
      password: '',
      role: 'SECRETAIRE',
      isActive: true,
      isGlobal: false,
      paroisseId: null,
      roleParoisse: 'SECRETAIRE',
    });
  };

  return (
    <div className="stack">
      <div className="button-row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <PageHeader title="Utilisateurs" subtitle="Gestion complète" />
        <button
          type="button"
          className={showForm ? 'btn btn-secondary' : 'btn btn-primary'}
          onClick={() => setShowForm(!showForm)}
          disabled={loading}
        >
          {showForm ? 'Annuler' : '+ Nouvel utilisateur'}
        </button>
      </div>

      {error ? (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      ) : null}

      {showForm ? (
        <form onSubmit={handleSubmit} className="card stack">
          <h3 style={{ margin: 0 }}>
            {editingId ? 'Modifier' : 'Créer'} un utilisateur
          </h3>

          {editingId ? (
            <div className="alert alert-info" role="status">
              Identité, coordonnées et mot de passe appartiennent au titulaire du compte : il les met
              à jour lui-même depuis « Mon profil ». Cet écran pilote le rôle, le périmètre et l’activation.
            </div>
          ) : null}

          <div className="form-grid">
            <div className="form-field">
              <label htmlFor="sa-user-nom">Nom *</label>
              <input
                id="sa-user-nom"
                className="input"
                type="text"
                name="nom"
                value={formData.nom}
                onChange={handleInputChange}
                required
                minLength={2}
                maxLength={100}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
              />
            </div>

            <div className="form-field">
              <label htmlFor="sa-user-prenom">Prénom *</label>
              <input
                id="sa-user-prenom"
                className="input"
                type="text"
                name="prenom"
                value={formData.prenom}
                onChange={handleInputChange}
                required
                minLength={2}
                maxLength={150}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
              />
            </div>

            <div className="form-field">
              <label htmlFor="sa-user-username">Username *</label>
              <input
                id="sa-user-username"
                className="input"
                type="text"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                required
                minLength={3}
                maxLength={100}
                pattern="[A-Za-z0-9._-]+"
                title="Lettres, chiffres, point, tiret et underscore uniquement"
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
              />
              {editingId ? (
                <small className="muted">Identifiant immuable après création.</small>
              ) : null}
            </div>

            <div className="form-field">
              <label htmlFor="sa-user-email">E-mail professionnel</label>
              <input
                id="sa-user-email"
                className="input"
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                maxLength={150}
                disabled
                readOnly
              />
              <small className="muted">
                {editingId
                  ? 'Adresse pro immuable.'
                  : 'Généré automatiquement selon le nom et la paroisse à la création.'}
              </small>
            </div>

            <div className="form-field">
              <label htmlFor="sa-user-telephone">Téléphone</label>
              <input
                id="sa-user-telephone"
                className="input"
                type="tel"
                name="telephone"
                value={formData.telephone}
                onChange={handleInputChange}
                maxLength={50}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
              />
            </div>

            {!editingId ? (
              <div className="form-field">
                <label htmlFor="sa-user-password">Mot de passe *</label>
                <input
                  id="sa-user-password"
                  className="input"
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  minLength={8}
                  maxLength={200}
                />
              </div>
            ) : (
              <div className="form-field">
                <p className="muted" style={{ margin: '0.5rem 0 0' }}>
                  Le mot de passe n’est pas modifiable depuis cet écran (sécurité plateforme).
                </p>
              </div>
            )}

            <div className="form-field">
              <label htmlFor="sa-user-role">Rôle *</label>
              <select
                id="sa-user-role"
                className="select input"
                name="role"
                value={formData.role}
                onChange={handleInputChange}
                disabled={Boolean(editingId) && editingIsGlobal}
              >
                <option value="SECRETAIRE">Secrétaire</option>
                <option value="CURE">Curé</option>
                <option value="ADMIN">Admin Local</option>
                <option
                  value="COMPTABLE"
                  disabled={Boolean(editingId) && editingIsGlobal === false}
                >
                  Comptable plateforme
                </option>
                <option
                  value="SUPER_ADMIN"
                  disabled={Boolean(editingId) && editingIsGlobal === false}
                >
                  Super Admin
                </option>
              </select>
            </div>
          </div>

          <label className="form-field" style={{ flexDirection: 'row', alignItems: 'center', gap: 8, cursor: isEditingSelf ? 'not-allowed' : 'pointer' }}>
            <input
              type="checkbox"
              name="isActive"
              checked={formData.isActive}
              onChange={handleInputChange}
              disabled={isEditingSelf}
            />
            <span>Actif</span>
          </label>
          {isEditingSelf ? (
            <small className="muted">Vous ne pouvez pas désactiver votre propre compte.</small>
          ) : null}

          <label className="form-field" style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <input
              type="checkbox"
              name="isGlobal"
              checked={formData.isGlobal}
              disabled
            />
            <span>Accès global (SUPER_ADMIN / COMPTABLE)</span>
          </label>

          {!formData.isGlobal && !editingId ? (
            <div className="users-scope-panel">
              <div className="form-field">
                <label htmlFor="sa-user-paroisse">Paroisse *</label>
                <select
                  id="sa-user-paroisse"
                  className="select input"
                  name="paroisseId"
                  value={formData.paroisseId || ''}
                  onChange={handleInputChange}
                  required
                >
                  <option value="">— Sélectionner une paroisse —</option>
                  {parishes.map((parish) => (
                    <option key={parish.publicId} value={parish.publicId}>
                      {parish.nom} ({parish.doyenneNom})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="sa-user-role-paroisse">Rôle dans la paroisse *</label>
                <select
                  id="sa-user-role-paroisse"
                  className="select input"
                  name="roleParoisse"
                  value={formData.roleParoisse}
                  onChange={handleInputChange}
                >
                  <option value="ADMIN">Admin Paroisse</option>
                  <option value="GESTIONNAIRE">Gestionnaire</option>
                  <option value="SECRETAIRE">Secrétaire</option>
                  <option value="CONSULTATION">Consultation</option>
                </select>
              </div>
            </div>
          ) : null}

          {editingId && !formData.isGlobal ? (
            <p className="muted" style={{ margin: 0 }}>
              L’affectation à la paroisse reste inchangée pendant cette modification.
            </p>
          ) : null}

          <div className="button-row">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Traitement…' : editingId ? 'Modifier' : 'Créer'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={handleCancel}>
              Annuler
            </button>
          </div>
        </form>
      ) : null}

      {loading && !error ? <p className="muted">Chargement…</p> : null}

      <AppTable
        columns={[
          ...columns,
          { key: 'actions', label: 'Actions' }
        ]}
        rows={rows.map(row => ({
          ...row,
          actions: (
            <div className="button-row">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => handleEdit(row.id)}
              >
                Modifier
              </button>
              {row.id === currentUserId ? (
                <span className="muted" style={{ alignSelf: 'center', fontSize: '0.8rem' }}>Vous</span>
              ) : (
                <button
                  type="button"
                  className="btn btn-danger btn-sm"
                  onClick={() => setPendingDeactivate(row.id)}
                  disabled={deactivating && pendingDeactivate === row.id}
                >
                  Désactiver
                </button>
              )}
            </div>
          )
        }))}
        renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          if (column.key === 'actions') return row.actions;
          return row[column.key];
        }}
      />

      <AppDialog
        open={Boolean(pendingDeactivate)}
        title="Désactiver l'utilisateur"
        confirmLabel="Désactiver"
        cancelLabel="Annuler"
        danger
        busy={deactivating}
        onCancel={() => setPendingDeactivate(null)}
        onConfirm={handleDelete}
      >
        <p style={{ margin: 0 }}>
          Êtes-vous sûr de vouloir désactiver cet utilisateur ?
        </p>
      </AppDialog>
    </div>
  );
}
