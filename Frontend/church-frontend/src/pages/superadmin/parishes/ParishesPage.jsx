import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppTable from '../../../components/ui/AppTable';
import AppBadge from '../../../components/ui/AppBadge';
import { parishService } from '../../../services/parish.service';
import { mapParoisseToTableRow } from '../../../utils/apiMappers';

const columns = [
  { key: 'name', label: 'Paroisse' },
  { key: 'city', label: 'Localité' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Téléphone' },
  { key: 'active', label: 'État' },
];

export default function ParishesPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    nom: '',
    adresse: '',
    email: '',
    telephone: '',
    isActive: true,
  });

  useEffect(() => {
    loadParishes();
  }, []);

  const loadParishes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await parishService.getAll();
      setRows((data || []).map(mapParoisseToTableRow));
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
      if (editingId) {
        await parishService.update(editingId, formData);
      } else {
        await parishService.create(formData);
      }
      await loadParishes();
      handleCancel();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = async (parishId) => {
    try {
      const parish = rows.find(row => row.id === parishId);
      if (parish) {
        setFormData({
          nom: parish.name || '',
          adresse: parish.address || '',
          email: parish.email || '',
          telephone: parish.phone || '',
          isActive: parish.active !== false,
        });
        setEditingId(parishId);
        setShowForm(true);
      }
    } catch (e) {
      setError('Erreur lors de la récupération');
    }
  };

  const handleDelete = async (parishId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir désactiver cette paroisse ?')) return;
    try {
      setLoading(true);
      await parishService.delete(parishId);
      await loadParishes();
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
      adresse: '',
      email: '',
      telephone: '',
      isActive: true,
    });
  };

  return (
    <div className="stack">
      <div className="flex items-center justify-between mb-4">
        <PageHeader title="Paroisses" subtitle="Gestion complète" />
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
          {showForm ? 'Annuler' : '+ Nouvelle Paroisse'}
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
            {editingId ? 'Modifier' : 'Créer'} une Paroisse
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
                Adresse *
              </label>
              <input
                type="text"
                name="adresse"
                value={formData.adresse}
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
                Email
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleInputChange}
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
                Téléphone
              </label>
              <input
                type="tel"
                name="telephone"
                value={formData.telephone}
                onChange={handleInputChange}
                style={{
                  width: '100%',
                  padding: '8px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px'
                }}
              />
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
