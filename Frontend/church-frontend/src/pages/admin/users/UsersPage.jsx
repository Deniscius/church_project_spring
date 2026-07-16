import { useState, useEffect } from 'react';
import { userService } from '../../../services/user.service';
import { useAuthStore } from '../../../store/auth.context';

const createInitialFormData = () => ({
  nom: '',
  prenom: '',
  username: '',
  password: '',
  role: 'SECRETAIRE',
  isActive: true,
  isGlobal: false,
  roleParoisse: 'SECRETAIRE',
});

/**
 * Page de gestion des utilisateurs (Admin)
 * CRUD complet : Create, Read, Update, Delete
 */
export function UsersPage() {
  const { selectedParoisse } = useAuthStore();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState(createInitialFormData);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getAll();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur lors du chargement des utilisateurs');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
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
        password: formData.password,
        role: formData.role,
        isActive: formData.isActive,
        isGlobal: false,
      };

      if (editingId) {
        await userService.update(editingId, payload);
      } else {
        if (!selectedParoisse?.paroisseId) {
          throw new Error(
            'Aucune paroisse active n’est disponible dans votre session. Reconnectez-vous avant de créer un utilisateur.'
          );
        }

        await userService.create({
          ...payload,
          paroisses: [
            {
              paroisseId: selectedParoisse.paroisseId,
              roleParoisse: formData.roleParoisse,
            },
          ],
        });
      }

      setShowForm(false);
      setFormData(createInitialFormData());
      setEditingId(null);
      await fetchUsers();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : editingId
            ? 'Erreur lors de la mise à jour'
            : 'Erreur lors de la création'
      );
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (userId) => {
    try {
      setError(null);
      const userToEdit = await userService.getById(userId);

      setFormData({
        nom: userToEdit.nom || '',
        prenom: userToEdit.prenom || '',
        username: userToEdit.username || '',
        password: '',
        role: userToEdit.role || 'SECRETAIRE',
        isActive: Boolean(userToEdit.isActive),
        isGlobal: false,
        roleParoisse: 'SECRETAIRE',
      });
      setEditingId(userId);
      setShowForm(true);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Erreur lors de la récupération de l’utilisateur'
      );
      console.error(err);
    }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await userService.delete(userId);
      await fetchUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur lors de la suppression');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setFormData(createInitialFormData());
    setEditingId(null);
  };

  return (
    <div className="users-page">
      <div className="page-header">
        <h1>Gestion des Utilisateurs</h1>
        <button
          className="btn btn-primary"
          onClick={() => setShowForm(!showForm)}
          disabled={loading}
        >
          {showForm ? 'Annuler' : '+ Nouvel Utilisateur'}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger">
          {error}
        </div>
      )}

      {showForm && (
        <div className="form-container">
          <h2>{editingId ? 'Modifier Utilisateur' : 'Créer Utilisateur'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label>Nom *</label>
                <input
                  type="text"
                  name="nom"
                  value={formData.nom}
                  onChange={handleInputChange}
                  required
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label>Prénom *</label>
                <input
                  type="text"
                  name="prenom"
                  value={formData.prenom}
                  onChange={handleInputChange}
                  required
                  disabled={loading}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Username *</label>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleInputChange}
                  required
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label>Mot de passe {editingId ? '' : '*'}</label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required={!editingId}
                  disabled={loading}
                  placeholder={editingId ? 'Laisser vide pour ne pas changer' : ''}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Rôle *</label>
                <select
                  name="role"
                  value={formData.role}
                  onChange={handleInputChange}
                  disabled={loading}
                >
                  <option value="ADMIN">Administrateur</option>
                  <option value="SECRETAIRE">Secrétaire</option>
                  <option value="CURE">Curé</option>
                </select>
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="isActive"
                    checked={formData.isActive}
                    onChange={handleInputChange}
                    disabled={loading}
                  />
                  {' '}Actif
                </label>
              </div>
            </div>

            {!editingId && (
              <div className="form-row">
                <div className="form-group">
                  <label>Paroisse</label>
                  <input
                    type="text"
                    value={selectedParoisse?.paroisseNom || 'Aucune paroisse active'}
                    disabled
                  />
                </div>
                <div className="form-group">
                  <label>Rôle dans la paroisse *</label>
                  <select
                    name="roleParoisse"
                    value={formData.roleParoisse}
                    onChange={handleInputChange}
                    disabled={loading}
                  >
                    <option value="ADMIN">Administrateur paroissial</option>
                    <option value="GESTIONNAIRE">Gestionnaire</option>
                    <option value="SECRETAIRE">Secrétaire</option>
                    <option value="CONSULTATION">Consultation</option>
                  </select>
                </div>
              </div>
            )}

            {editingId && (
              <p>
                L’affectation paroissiale reste inchangée pendant cette modification.
              </p>
            )}

            <div className="form-actions">
              <button
                type="submit"
                className="btn btn-success"
                disabled={loading}
              >
                {loading ? 'Enregistrement...' : (editingId ? 'Modifier' : 'Créer')}
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleCancel}
                disabled={loading}
              >
                Annuler
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="users-list">
        {loading && !showForm ? (
          <div className="loading">Chargement...</div>
        ) : users.length === 0 ? (
          <div className="empty-state">
            <p>Aucun utilisateur trouvé. Créez le premier en cliquant sur "Nouvel Utilisateur".</p>
          </div>
        ) : (
          <table className="table table-striped">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Username</th>
                <th>Rôle</th>
                <th>Statut</th>
                <th>Paroisse</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.publicId}>
                  <td>{u.nom} {u.prenom}</td>
                  <td>{u.username}</td>
                  <td>{u.role}</td>
                  <td>
                    <span className={`badge ${u.isActive ? 'badge-success' : 'badge-danger'}`}>
                      {u.isActive ? 'Actif' : 'Inactif'}
                    </span>
                  </td>
                  <td>{selectedParoisse?.paroisseNom || '—'}</td>
                  <td className="actions">
                    <button
                      className="btn btn-sm btn-info"
                      onClick={() => handleEdit(u.publicId)}
                      disabled={loading}
                    >
                      Modifier
                    </button>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDelete(u.publicId)}
                      disabled={loading}
                    >
                      Supprimer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default UsersPage;
