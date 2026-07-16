import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import { useTenant } from '../../../hooks/useTenant';
import { scheduleService } from '../../../services/schedule.service';

const DAYS = ['DIMANCHE', 'LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI'];
const INITIAL_VALUE = {
  libelle: '',
  jourSemaine: 'DIMANCHE',
  heureCelebration: '',
  isActive: true,
};

export default function ScheduleForm({ scheduleId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(scheduleId));
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!scheduleId) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await scheduleService.getById(scheduleId);
        if (!cancelled) {
          setForm({
            libelle: data.libelle || '',
            jourSemaine: data.jourSemaine || 'DIMANCHE',
            heureCelebration: data.heureCelebration?.slice(0, 5) || '',
            isActive: data.isActive !== false,
          });
        }
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Horaire introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [scheduleId]);

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) return setError('Aucune paroisse active');
    try {
      setLoading(true);
      setError(null);
      const payload = { ...form, paroissePublicId: activeParish.id };
      if (scheduleId) await scheduleService.update(scheduleId, payload);
      else await scheduleService.create(payload);
      navigate('/admin/horaires');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard title={scheduleId ? 'Horaire existant' : 'Nouvel horaire'}>
      {error ? <p className="text-red-600">{error}</p> : null}
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="schedule-label">Libellé</label>
            <AppInput id="schedule-label" value={form.libelle} maxLength={150}
              onChange={(e) => setForm({ ...form, libelle: e.target.value })} />
          </div>
          <div className="form-field">
            <label htmlFor="schedule-time">Heure *</label>
            <AppInput id="schedule-time" type="time" required value={form.heureCelebration}
              onChange={(e) => setForm({ ...form, heureCelebration: e.target.value })} />
          </div>
          <div className="form-field">
            <label htmlFor="schedule-day">Jour *</label>
            <select id="schedule-day" className="select" value={form.jourSemaine}
              onChange={(e) => setForm({ ...form, jourSemaine: e.target.value })}>
              {DAYS.map((day) => <option key={day} value={day}>{day}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="schedule-active">État *</label>
            <select id="schedule-active" className="select" value={String(form.isActive)}
              onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}>
              <option value="true">Actif</option><option value="false">Inactif</option>
            </select>
          </div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>{loading ? 'Enregistrement…' : 'Enregistrer'}</AppButton>
          <AppButton variant="secondary" onClick={() => navigate('/admin/horaires')}>Annuler</AppButton>
        </div>
      </form>
    </AppCard>
  );
}
