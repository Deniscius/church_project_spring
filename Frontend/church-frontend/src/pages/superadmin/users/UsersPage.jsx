import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { userService } from '../../../services/user.service';
import { parishService } from '../../../services/parish.service';
import { mapUserToRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'firstName', label: 'Prénom' },
  { key: 'lastName', label: 'Nom' },
  { key: 'username', label: 'Username' },
  { key: 'role', label: 'Rôle' },
  { key: 'active', label: 'État' },
];

export default function UsersPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [parishes, setParishes] = useState([]);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    username: '',
    password: '',
    role: 'SECRETAIRE',
    isActive: true,
    isGlobal: false,
    paroisseId: null,
    roleParoisse: 'SECRETAIRE',
  });

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
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      const payload = {
        nom: formData.nom,
        prenom: formData.prenom,
        username: formData.username,
        password: formData.password,
        role: formData.role,
        isActive: formData.isActive,
        isGlobal: formData.isGlobal,
      };
      
      let userId;
      if (editingId) {
        await userService.update(editingId, payload);
        userId = editingId;
      } else {
        const result = await userService.create(payload);
        userId = result?.publicId || result?.id;
      }

      // Assigner à une paroisse si sélectionnée et pas global
      if (userId && formData.paroisseId && !formData.isGlobal) {
        await userService.assignParoisse(userId, {
          paroisseId: formData.paroisseId,
          roleParoisse: formData.roleParoisse,
        });
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
          password: '',
          role: user.role || 'SECRETAIRE',
          isActive: user.active !== false,
          isGlobal: user.isGlobal || false,
        });
        setEditingId(userId);
        setShowForm(true);
      }
    } catch (e) {
      setError('Erreur lors de la récupération');
    }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir désactiver cet utilisateur ?')) return;
    try {
      setLoading(true);
      await userService.delete(userId);
      await loadUsers();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setFormData({
      nom: '',
      prenom: '',
      username: '',
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
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
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
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
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
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Mot de passe {editingId ? '(laisser vide pour garder)' : '*'}
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                required={!editingId}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                Rôle *
              </label>
              <select
                name="role"
                value={formData.role}
                onChange={handleInputChange}
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
                <option value="SUPER_ADMIN">Super Admin</option>
              </select>
            </div>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                name="isActive"
                checked={formData.isActive}
                onChange={handleInputChange}
              />
              <span style={{ fontWeight: 'bold' }}>Actif</span>
            </label>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                name="isGlobal"
                checked={formData.isGlobal}
                onChange={handleInputChange}
              />
              <span style={{ fontWeight: 'bold' }}>Administrateur Global (accès à toutes les paroisses)</span>
            </label>
          </div>

          {!formData.isGlobal && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px', backgroundColor: '#e8f4f8', padding: '12px', borderRadius: '4px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                  Paroisse ({formData.isGlobal ? 'N/A pour Admin Global' : '*'})
                </label>
                <select
                  name="paroisseId"
                  value={formData.paroisseId || ''}
                  onChange={handleInputChange}
                  required={!formData.isGlobal}
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
                      {parish.nom} ({parish.localiteVille})
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
                  disabled={formData.isGlobal}
                  style={{
                    width: '100%',
                    padding: '8px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    fontSize: '14px'
                  }}
                >
                  <option value="ADMIN">Admin Paroisse</option>
                  <option value="SECRETAIRE">Secrétaire</option>
                  <option value="CURE">Curé</option>
                </select>
              </div>
            </div>
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
              <button
                onClick={() => handleDelete(row.id)}
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
            </div>
          )
        }))}
        renderCell={(row, column) => {
          if (column.key === 'active') return <AppBadge value={row.active} />;
          if (column.key === 'actions') return row.actions;
          return row[column.key];
        }}
      />
    </div>
  );
}
