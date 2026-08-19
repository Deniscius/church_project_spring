import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import { useTenant } from '../../../hooks/useTenant';
import { useScrollToError } from '../../../hooks/useScrollToError';
import { scheduleService } from '../../../services/schedule.service';
import { NATURE_FORFAIT_OPTIONS, WEEK_DAYS, WEEK_DAY_LABELS } from '../../../constants/enums';
import { getDayEnumFromDateString } from '../../../utils/schedulingUtils';
import FormError from '../../../components/ui/FormError';
import { qk } from '../../../hooks/queries/usePublicReferentiel';
import { parishProgrammeKeys } from '../../../hooks/queries/useParishProgramme';

const INITIAL_VALUE = {
  mode: 'hebdo', // hebdo | solennel
  libelle: '',
  jourSemaine: 'DIMANCHE',
  heureCelebration: '',
  isActive: true,
  dateSpecifique: '',
  uniqueSurParoisse: false,
  natureHonoraire: 'SPECIALE',
};

function suggestedNatureForDay(dayEnum) {
  return dayEnum === 'DIMANCHE' ? 'DOMINICALE' : 'NORMALE';
}

export default function ScheduleForm({ scheduleId = null }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { activeParish } = useTenant();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(scheduleId));
  const [error, setError] = useState(null);
  const errorRef = useScrollToError(error);

  useEffect(() => {
    if (!scheduleId) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await scheduleService.getById(scheduleId);
        if (!cancelled) {
          const hasDate = Boolean(data.dateSpecifique);
          setForm({
            mode: hasDate ? 'solennel' : 'hebdo',
            libelle: data.libelle || '',
            jourSemaine: data.jourSemaine || 'DIMANCHE',
            heureCelebration: data.heureCelebration?.slice(0, 5) || '',
            isActive: data.isActive !== false,
            dateSpecifique: data.dateSpecifique || '',
            uniqueSurParoisse: Boolean(data.uniqueSurParoisse),
            natureHonoraire: data.natureHonoraire
              || (hasDate ? suggestedNatureForDay(data.jourSemaine) : 'SPECIALE'),
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

  const dayFromDate = useMemo(
    () => (form.dateSpecifique ? getDayEnumFromDateString(form.dateSpecifique) : null),
    [form.dateSpecifique]
  );

  const setMode = (mode) => {
    setForm((current) => {
      if (mode === 'hebdo') {
        return {
          ...current,
          mode,
          dateSpecifique: '',
          uniqueSurParoisse: false,
          natureHonoraire: 'SPECIALE',
        };
      }
      return {
        ...current,
        mode,
        // Une date précise complète la grille hebdomadaire par défaut.
        // Le remplacement total doit être un choix explicite de l'admin.
        uniqueSurParoisse: false,
        natureHonoraire: current.dateSpecifique
          ? (current.natureHonoraire || suggestedNatureForDay(getDayEnumFromDateString(current.dateSpecifique)))
          : 'SPECIALE',
      };
    });
  };

  const setDateSpecifique = (value) => {
    const day = value ? getDayEnumFromDateString(value) : null;
    setForm((current) => ({
      ...current,
      dateSpecifique: value,
      jourSemaine: day || current.jourSemaine,
      // Par défaut honoraire selon le jour ; l’admin peut forcer SPECIALE (solennité).
      natureHonoraire: value
        ? (current.natureHonoraire === 'SPECIALE' ? 'SPECIALE' : suggestedNatureForDay(day))
        : current.natureHonoraire,
      uniqueSurParoisse: value ? current.uniqueSurParoisse : false,
    }));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) return setError('Aucune paroisse active');

    const isSolennel = form.mode === 'solennel';
    if (isSolennel && !form.dateSpecifique) {
      return setError('Indiquez la date de l’événement solennel / célébration spéciale.');
    }
    if (form.uniqueSurParoisse && !form.dateSpecifique) {
      return setError('Une messe unique nécessite une date précise.');
    }

    try {
      setLoading(true);
      setError(null);
      const payload = {
        libelle: form.libelle,
        jourSemaine: isSolennel
          ? (dayFromDate || form.jourSemaine)
          : form.jourSemaine,
        heureCelebration: form.heureCelebration,
        isActive: form.isActive,
        paroissePublicId: activeParish.id,
        dateSpecifique: isSolennel ? form.dateSpecifique : null,
        uniqueSurParoisse: Boolean(isSolennel && form.uniqueSurParoisse),
        natureHonoraire: isSolennel ? form.natureHonoraire : null,
      };
      if (scheduleId) await scheduleService.update(scheduleId, payload);
      else await scheduleService.create(payload);
      await queryClient.invalidateQueries({ queryKey: qk.horairesPublicActives });
      if (activeParish?.id) {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: qk.horairesParish(activeParish.id) }),
          queryClient.invalidateQueries({ queryKey: parishProgrammeKeys.all(activeParish.id) }),
        ]);
      }
      navigate('/admin/horaires');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  const isSolennel = form.mode === 'solennel';

  return (
    <AppCard
      title={scheduleId ? 'Modifier le créneau' : 'Nouveau créneau'}
      subtitle="Créneau hebdomadaire récurrent, ou créneau à date précise. Une date précise s’ajoute aux horaires habituels tant que « célébration unique » n’est pas cochée."
    >
      <FormError error={error} errorRef={errorRef} />
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field full">
            <span className="muted" style={{ display: 'block', marginBottom: 8 }}>Type de créneau</span>
            <div className="button-row" role="group" aria-label="Type de créneau">
              <AppButton
                type="button"
                variant={form.mode === 'hebdo' ? 'primary' : 'secondary'}
                onClick={() => setMode('hebdo')}
              >
                Hebdomadaire
              </AppButton>
              <AppButton
                type="button"
                variant={isSolennel ? 'primary' : 'secondary'}
                onClick={() => setMode('solennel')}
              >
                Événement solennel / date précise
              </AppButton>
            </div>
          </div>

          <div className="form-field">
            <label htmlFor="schedule-label">Libellé</label>
            <AppInput
              id="schedule-label"
              value={form.libelle}
              maxLength={150}
              placeholder={isSolennel ? 'Ex. Solennité de Marie Auxiliatrice' : 'Ex. Messe du matin'}
              onChange={(e) => setForm({ ...form, libelle: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor="schedule-time">Heure *</label>
            <AppInput
              id="schedule-time"
              type="time"
              required
              value={form.heureCelebration}
              onChange={(e) => setForm({ ...form, heureCelebration: e.target.value })}
            />
          </div>

          {isSolennel ? (
            <>
              <div className="form-field">
                <label htmlFor="schedule-date">Date de l’événement *</label>
                <AppInput
                  id="schedule-date"
                  type="date"
                  required
                  value={form.dateSpecifique}
                  onChange={(e) => setDateSpecifique(e.target.value)}
                />
                <small className="muted">
                  N’importe quel jour du calendrier (pas limité aux jours habituels du type de demande).
                  {dayFromDate ? ` → ${WEEK_DAY_LABELS[dayFromDate] || dayFromDate}` : ''}
                </small>
              </div>
              <div className="form-field">
                <label htmlFor="schedule-honoraire">Honoraire à payer *</label>
                <select
                  id="schedule-honoraire"
                  className="select"
                  required
                  value={form.natureHonoraire}
                  onChange={(e) => setForm({ ...form, natureHonoraire: e.target.value })}
                >
                  {NATURE_FORFAIT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                      {option.value === suggestedNatureForDay(dayFromDate)
                        ? ' (suggéré pour ce jour)'
                        : ''}
                      {option.value === 'SPECIALE' ? ' — solennité' : ''}
                    </option>
                  ))}
                </select>
                <small className="muted">
                  Détermine le forfait (montant) proposé aux fidèles pour cette célébration.
                  Dimanche → dominicale, jour de semaine → normale, solennité → spéciale.
                </small>
              </div>
              <div className="form-field full">
                <label htmlFor="schedule-unique">
                  <input
                    id="schedule-unique"
                    type="checkbox"
                    checked={Boolean(form.uniqueSurParoisse)}
                    onChange={(e) => setForm({ ...form, uniqueSurParoisse: e.target.checked })}
                    style={{ marginRight: 8 }}
                  />
                  Célébration unique sur la paroisse ce jour-là
                </label>
                <small className="muted">
                  Laissez décoché pour conserver les créneaux hebdomadaires du jour et ajouter cette célébration.
                  Cochez uniquement si cette messe doit remplacer tout le programme habituel de cette date.
                </small>
              </div>
            </>
          ) : (
            <div className="form-field">
              <label htmlFor="schedule-day">Jour de la semaine *</label>
              <select
                id="schedule-day"
                className="select"
                value={form.jourSemaine}
                onChange={(e) => setForm({ ...form, jourSemaine: e.target.value })}
              >
                {WEEK_DAYS.map((day) => (
                  <option key={day} value={day}>{WEEK_DAY_LABELS[day] || day}</option>
                ))}
              </select>
            </div>
          )}

          <div className="form-field">
            <label htmlFor="schedule-active">État *</label>
            <select
              id="schedule-active"
              className="select"
              value={String(form.isActive)}
              onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}
            >
              <option value="true">Actif</option>
              <option value="false">Inactif</option>
            </select>
          </div>
        </div>
        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" disabled={loading}>
            {loading ? 'Enregistrement…' : 'Enregistrer'}
          </AppButton>
          <AppButton variant="secondary" onClick={() => navigate('/admin/horaires')}>
            Annuler
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
