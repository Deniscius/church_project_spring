import { useState, useEffect } from 'react';
import { parishService } from '../../../services/parish.service';
import { useAuthStore } from '../../../store/auth.context';

/**
 * Page de gestion des paroisses (Super Admin)
 * CRUD complet : Create, Read, Update, Delete
 */
export function ParishesPage() {
  const { user } = useAuthStore();
  const [parishes, setParishes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    nom: '',
    adresse: '',
    email: '',
    telephone: '',
    isActive: true,
  });
  const [editingId, setEditingId] = useState(null);

  // Charger la liste des paroisses
  useEffect(() => {
    fetchParishes();
  }, []);

  const fetchParishes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await parishService.getAll();
      setParishes(Array.isArray(data) ? data : []);
    } catch (err) {
      setError('Erreur lors du chargement des paroisses');
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
        await parishService.update(editingId, formData);
      } else {
        // Création
        await parishService.create(formData);
      }

      setShowForm(false);
      setFormData({
        nom: '',
        adresse: '',
        email: '',
        telephone: '',
        isActive: true,
      });
      setEditingId(null);
      await fetchParishes();
    } catch (err) {
      setError(editingId ? 'Erreur lors de la mise à jour' : 'Erreur lors de la création');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (paroisseId) => {
    try {
      const parochToEdit = await parishService.getById(paroisseId);
      setFormData(parochToEdit);
      setEditingId(paroisseId);
      setShowForm(true);
    } catch (err) {
      setError('Erreur lors de la récupération de la paroisse');
      console.error(err);
    }
  };

  const handleDelete = async (paroisseId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir désactiver cette paroisse ?')) {
      return;
    }

    try {
      setLoading(true);
      await parishService.delete(paroisseId);
      await fetchParishes();
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
      adresse: '',
      email: '',
      telephone: '',
      isActive: true,
    });
    setEditingId(null);
  };

  return (
    <div className="parishes-page">
      <div className="page-header">
        <h1>Gestion des Paroisses</h1>
        <button 
          className="btn btn-primary"
          onClick={() => setShowForm(!showForm)}
          disabled={loading}
        >
          {showForm ? 'Annuler' : '+ Nouvelle Paroisse'}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger">
          {error}
        </div>
      )}

      {showForm && (
        <div className="form-container">
          <h2>{editingId ? 'Modifier Paroisse' : 'Créer Paroisse'}</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Nom de la Paroisse *</label>
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
              <label>Adresse *</label>
              <input
                type="text"
                name="adresse"
                value={formData.adresse}
                onChange={handleInputChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Email</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label>Téléphone</label>
                <input
                  type="tel"
                  name="telephone"
                  value={formData.telephone}
                  onChange={handleInputChange}
                  disabled={loading}
                />
              </div>
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

      <div className="parishes-list">
        {loading && !showForm ? (
          <div className="loading">Chargement...</div>
        ) : parishes.length === 0 ? (
          <div className="empty-state">
            <p>Aucune paroisse trouvée. Créez la première en cliquant sur "Nouvelle Paroisse".</p>
          </div>
        ) : (
          <table className="table table-striped">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Adresse</th>
                <th>Email</th>
                <th>Téléphone</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {parishes.map((p) => (
                <tr key={p.id}>
                  <td>{p.nom}</td>
                  <td>{p.adresse}</td>
                  <td>{p.email}</td>
                  <td>{p.telephone}</td>
                  <td>
                    <span className={`badge ${p.isActive ? 'badge-success' : 'badge-danger'}`}>
                      {p.isActive ? 'Actif' : 'Inactif'}
                    </span>
                  </td>
                  <td className="actions">
                    <button
                      className="btn btn-sm btn-info"
                      onClick={() => handleEdit(p.id)}
                      disabled={loading}
                    >
                      Modifier
                    </button>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDelete(p.id)}
                      disabled={loading}
                    >
                      Désactiver
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

export default ParishesPage;
