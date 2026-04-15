import { useState, useEffect } from 'react';
import { userService } from '../../../services/user.service';
import { useAuthStore } from '../../../store/auth.context';

/**
 * Page de gestion des utilisateurs (Admin)
 * CRUD complet : Create, Read, Update, Delete
 */
export function UsersPage() {
  const { user, token } = useAuthStore();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    username: '',
    password: '',
    role: 'SECRETAIRE',
    isActive: true,
    isGlobal: false,
    paroisses: [],
  });
  const [editingId, setEditingId] = useState(null);

  // Charger la liste des utilisateurs
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
      setError('Erreur lors du chargement des utilisateurs');
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

      if (editingId) {
        // Mise à jour
        await userService.update(editingId, formData);
      } else {
        // Création
        await userService.create(formData);
      }

      setShowForm(false);
      setFormData({
        nom: '',
        prenom: '',
        username: '',
        password: '',
        role: 'SECRETAIRE',
        isActive: true,
        isGlobal: false,
        paroisses: [],
      });
      setEditingId(null);
      await fetchUsers();
    } catch (err) {
      setError(editingId ? 'Erreur lors de la mise à jour' : 'Erreur lors de la création');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (userId) => {
    try {
      const userToEdit = await userService.getById(userId);
      setFormData(userToEdit);
      setEditingId(userId);
      setShowForm(true);
    } catch (err) {
      setError('Erreur lors de la récupération de l\'utilisateur');
      console.error(err);
    }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
      return;
    }

    try {
      setLoading(true);
      await userService.delete(userId);
      await fetchUsers();
    } catch (err) {
      setError('Erreur lors de la suppression');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setFormData({
      nom: '',
      prenom: '',
      username: '',
      password: '',
      role: 'SECRETAIRE',
      isActive: true,
      isGlobal: false,
      paroisses: [],
    });
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
                <label>Mot de passe *</label>
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
                <th>Paroisses</th>
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
                  <td>
                    {u.paroisses && u.paroisses.length > 0 ? (
                      <ul className="list-unstyled">
                        {u.paroisses.map((p) => (
                          <li key={p.id}>{p.paroisseNom} ({p.roleParoisse})</li>
                        ))}
                      </ul>
                    ) : (
                      <em>Aucune</em>
                    )}
                  </td>
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
