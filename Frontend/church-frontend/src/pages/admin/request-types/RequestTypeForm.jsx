import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppTextarea from '../../../components/ui/AppTextarea';
import WeekDaySelector from '../../../components/ui/WeekDaySelector';
import { WEEK_DAYS } from '../../../constants/enums';
import { useTenant } from '../../../hooks/useTenant';
import { requestTypeService } from '../../../services/requestType.service';

const CATEGORIES = ['EUCHARISTIE', 'SACRAMENT', 'SACRAMENTAUX'];
const INITIAL_VALUE = {
  libelle: '',
  description: '',
  typeDemandeEnum: 'EUCHARISTIE',
  isActive: true,
  delaiMinimumHeures: 24,
  joursCelebrationAutorises: ['DIMANCHE'],
};

export default function RequestTypeForm({ requestTypeId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(requestTypeId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!requestTypeId) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await requestTypeService.getById(requestTypeId);
        if (!cancelled) {
          setForm({
            libelle: data.libelle || '',
            description: data.description || '',
            typeDemandeEnum: data.typeDemandeEnum || 'EUCHARISTIE',
            isActive: data.isActive !== false,
            delaiMinimumHeures: data.delaiMinimumHeures ?? 24,
            joursCelebrationAutorises: data.joursCelebrationAutorises?.length
              ? [...data.joursCelebrationAutorises].sort(
                  (a, b) => WEEK_DAYS.indexOf(a) - WEEK_DAYS.indexOf(b)
                )
              : ['DIMANCHE'],
          });
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Type de demande introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [requestTypeId]);

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) return setError('Aucune paroisse active');
    if (!form.joursCelebrationAutorises.length) {
      return setError('Sélectionnez au moins un jour de célébration');
    }
    try {
      setLoading(true);
      setError(null);
      const payload = { ...form, paroissePublicId: activeParish.id };
      if (requestTypeId) await requestTypeService.update(requestTypeId, payload);
      else await requestTypeService.create(payload);
      navigate('/admin/types-demandes');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={requestTypeId ? 'Type de demande existant' : 'Nouveau type de demande'}>
      {error ? <p className="text-red-600">{error}</p> : null}
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="type-label">Libellé *</label>
            <AppInput id="type-label" required value={form.libelle}
              onChange={(e) => setForm({ ...form, libelle: e.target.value })} />
          </div>
          <div className="form-field">
            <label htmlFor="type-category">Catégorie *</label>
            <select id="type-category" className="select" value={form.typeDemandeEnum}
              onChange={(e) => setForm({ ...form, typeDemandeEnum: e.target.value })}>
              {CATEGORIES.map((category) => <option key={category} value={category}>{category}</option>)}
            </select>
          </div>
          <div className="form-field full">
            <label htmlFor="type-description">Description</label>
            <AppTextarea id="type-description" value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="form-field full">
            <label htmlFor="type-celebration-days">Jours de célébration autorisés *</label>
            <WeekDaySelector
              id="type-celebration-days"
              value={form.joursCelebrationAutorises}
              onChange={(joursCelebrationAutorises) => setForm({ ...form, joursCelebrationAutorises })}
            />
            <small className="muted">
              Seules les dates correspondant à ces jours seront proposées aux fidèles.
            </small>
          </div>
          <div className="form-field">
            <label htmlFor="type-lead-time">Délai minimum avant célébration (heures) *</label>
            <AppInput id="type-lead-time" type="number" required min="0" max="8760"
              value={form.delaiMinimumHeures}
              onChange={(e) => setForm({ ...form, delaiMinimumHeures: Number(e.target.value) })} />
            <small className="muted">Exemple : 24 impose un dépôt au moins un jour avant.</small>
          </div>
          <div className="form-field">
            <label htmlFor="type-active">État *</label>
            <select id="type-active" className="select" value={String(form.isActive)}
              onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}>
              <option value="true">Actif</option><option value="false">Inactif</option>
            </select>
          </div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>{loading ? 'Enregistrement…' : 'Enregistrer'}</AppButton>
          <AppButton variant="secondary" onClick={() => navigate('/admin/types-demandes')}>Annuler</AppButton>
        </div>
      </form>
    </AppCard>
  );
}
