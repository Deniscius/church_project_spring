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
      <div className="flex items-center justify-between mb-4">
        <PageHeader title="Utilisateurs" subtitle="Gestion complète" />
        <button
          onClick={() => setShowForm(!showForm)}
          disabled={loading}
          style={{
            padding: '8px 16px',
            backgroundColor: '#006bb3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: 'bold'
          }}
        >
          {showForm ? 'Annuler' : '+ Nouvel Utilisateur'}
        </button>
      </div>

      {error && (
        <div style={{
          padding: '12px',
          backgroundColor: '#fee',
          color: '#c33',
          borderRadius: '4px',
          marginBottom: '16px'
        }}>
          {error}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} style={{
          backgroundColor: '#f5f5f5',
          padding: '20px',
          borderRadius: '8px',
          marginBottom: '24px'
        }}>
          <h3 style={{ margin: '0 0 16px 0' }}>
            {editingId ? 'Modifier' : 'Créer'} un Utilisateur
          </h3>

          {editingId ? (
            <div className="alert-info" role="status" style={{ marginBottom: 16 }}>
              Identité, coordonnées et mot de passe appartiennent au titulaire du compte : il les met
              à jour lui-même depuis « Mon profil ». Cet écran pilote le rôle, le périmètre et l’activation.
            </div>
          ) : null}
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Nom *
              </label>
              <input
                type="text"
                name="nom"
                value={formData.nom}
                onChange={handleInputChange}
                required
                minLength={2}
                maxLength={100}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: editingId ? '#f5f5f5' : undefined,
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Prénom *
              </label>
              <input
                type="text"
                name="prenom"
                value={formData.prenom}
                onChange={handleInputChange}
                required
                minLength={2}
                maxLength={150}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: editingId ? '#f5f5f5' : undefined,
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Username *
              </label>
              <input
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
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: editingId ? '#f5f5f5' : undefined,
                }}
              />
              {editingId ? (
                <small style={{ color: '#666' }}>Identifiant immuable après création.</small>
              ) : null}
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                E-mail professionnel
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
                maxLength={150}
                disabled
                readOnly
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: '#f5f5f5',
                }}
              />
              <small style={{ color: '#666' }}>
                {editingId
                  ? 'Adresse pro immuable.'
                  : 'Généré automatiquement selon le nom et la paroisse à la création.'}
              </small>
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Téléphone
              </label>
              <input
                type="tel"
                name="telephone"
                value={formData.telephone}
                onChange={handleInputChange}
                maxLength={50}
                disabled={Boolean(editingId)}
                readOnly={Boolean(editingId)}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: editingId ? '#f5f5f5' : undefined,
                }}
              />
            </div>

            {!editingId ? (
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Mot de passe *
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                required
                minLength={8}
                maxLength={200}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
                }}
              />
            </div>
            ) : (
              <div style={{ padding: '8px 0', color: '#666', fontSize: '13px' }}>
                Le mot de passe n’est pas modifiable depuis cet écran (sécurité plateforme).
              </div>
            )}

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Rôle *
              </label>
              <select
                name="role"
                value={formData.role}
                onChange={handleInputChange}
                disabled={Boolean(editingId) && editingIsGlobal}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
                }}
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

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: isEditingSelf ? 'not-allowed' : 'pointer' }}>
              <input
                type="checkbox"
                name="isActive"
                checked={formData.isActive}
                onChange={handleInputChange}
                disabled={isEditingSelf}
              />
              <span style={{ fontWeight: 'bold' }}>Actif</span>
            </label>
            {isEditingSelf ? (
              <small style={{ color: '#666' }}>Vous ne pouvez pas désactiver votre propre compte.</small>
            ) : null}
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <input
                type="checkbox"
                name="isGlobal"
                checked={formData.isGlobal}
                disabled
              />
              <span style={{ fontWeight: 'bold' }}>
                Accès global (SUPER_ADMIN / COMPTABLE)
              </span>
            </label>
          </div>

          {!formData.isGlobal && !editingId && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px', backgroundColor: '#e8f4f8', padding: '12px', borderRadius: '4px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                  Paroisse *
                </label>
                <select
                  name="paroisseId"
                  value={formData.paroisseId || ''}
                  onChange={handleInputChange}
                  required
                  style={{
                    width: '100%',
                    padding: '8px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    fontSize: '14px'
                  }}
                >
                  <option value="">-- Sélectionner une paroisse --</option>
                  {parishes.map((parish) => (
                    <option key={parish.publicId} value={parish.publicId}>
                      {parish.nom} ({parish.doyenneNom})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                  Rôle dans la Paroisse *
                </label>
                <select
                  name="roleParoisse"
                  value={formData.roleParoisse}
                  onChange={handleInputChange}
                  style={{
                    width: '100%',
                    padding: '8px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    fontSize: '14px'
                  }}
                >
                  <option value="ADMIN">Admin Paroisse</option>
                  <option value="GESTIONNAIRE">Gestionnaire</option>
                  <option value="SECRETAIRE">Secrétaire</option>
                  <option value="CONSULTATION">Consultation</option>
                </select>
              </div>
            </div>
          )}

          {editingId && !formData.isGlobal && (
            <p style={{ marginBottom: '16px', color: '#666', fontSize: '13px' }}>
              L’affectation à la paroisse reste inchangée pendant cette modification.
            </p>
          )}

          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="submit"
              disabled={loading}
              style={{
                padding: '8px 16px',
                backgroundColor: '#006bb3',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              {loading ? 'Traitement...' : editingId ? 'Modifier' : 'Créer'}
            </button>
            <button
              type="button"
              onClick={handleCancel}
              style={{
                padding: '8px 16px',
                backgroundColor: '#ddd',
                color: '#333',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Annuler
            </button>
          </div>
        </form>
      )}

      {loading && !error && <p style={{ color: '#666' }}>Chargement…</p>}
      
      <AppTable
        columns={[
          ...columns,
          { key: 'actions', label: 'Actions' }
        ]}
        rows={rows.map(row => ({
          ...row,
          actions: (
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => handleEdit(row.id)}
                style={{
                  padding: '4px 12px',
                  backgroundColor: '#f9a825',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '12px'
                }}
              >
                Modifier
              </button>
              {row.id === currentUserId ? (
                <span style={{ fontSize: '12px', color: '#666', alignSelf: 'center' }}>Vous</span>
              ) : (
                <button
                  onClick={() => setPendingDeactivate(row.id)}
                  disabled={deactivating && pendingDeactivate === row.id}
                  style={{
                    padding: '4px 12px',
                    backgroundColor: '#d9534f',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '12px'
                  }}
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
