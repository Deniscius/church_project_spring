import React, { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import AppInput from '../../../components/ui/AppInput';
import AppSelect from '../../../components/ui/AppSelect';
import { useTenant } from '../../../hooks/useTenant';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import { ROUTES } from '../../../constants/routes';
import { formatParishTimeInUserZone } from '../../../utils/formatTime';
import { dashboardService } from '../../../services/dashboard.service';
import { scheduleService } from '../../../services/schedule.service';
import {
  parishProgrammeKeys,
  useParishProgrammeQuery,
} from '../../../hooks/queries/useParishProgramme';
import { qk } from '../../../hooks/queries/usePublicReferentiel';
import {
  useInvalidateParishCelebrations,
  useParishCelebrationsForDay,
} from '../../../hooks/queries/useParishUpcomingCelebrations';

const PROGRAMME_MODE_LABELS = {
  HEBDOMADAIRE: 'Programme hebdomadaire par défaut',
  HEBDOMADAIRE_AVEC_AJOUT: 'Programme habituel + créneau ponctuel',
  PERSONNALISE: 'Programme personnalisé pour cette date',
  MESSE_UNIQUE: 'Messe unique pour cette date',
};

function longDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return iso;
  }
}

function todayInParishZone() {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Africa/Lome' });
}

function draftFromSchedule(daySchedule) {
  const rows = (daySchedule?.creneaux || []).map((slot) => ({
    heureCelebration: slot.heureCelebration ? String(slot.heureCelebration).slice(0, 5) : '',
    libelle: slot.libelle || '',
    natureHonoraire: slot.natureHonoraire || null,
  }));
  return rows.length ? rows : [{ heureCelebration: '', libelle: '', natureHonoraire: null }];
}

export default function DailyProgrammePage() {
  const { date } = useParams();
  const queryClient = useQueryClient();
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const canEdit = has(PERMISSIONS.DEMAND_EDIT);
  const canManageSchedule = has(PERMISSIONS.SCHEDULE_MANAGE);
  const invalidate = useInvalidateParishCelebrations();
  const [editing, setEditing] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState('');
  const [saving, setSaving] = useState(false);
  const [editingDaySchedule, setEditingDaySchedule] = useState(false);
  const [daySlotsDraft, setDaySlotsDraft] = useState([]);
  const [savingDaySchedule, setSavingDaySchedule] = useState(false);
  const [confirmResetSchedule, setConfirmResetSchedule] = useState(false);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);

  const parishId = activeParish?.id;
  const {
    data: celebrations = [],
    isLoading,
    error: queryError,
    refetch,
  } = useParishCelebrationsForDay(parishId, date);
  const {
    data: scheduleDays = [],
    isLoading: scheduleLoading,
    error: scheduleError,
    refetch: refetchSchedule,
  } = useParishProgrammeQuery(parishId, { debut: date, fin: date });

  const daySchedule = useMemo(
    () => (scheduleDays || []).find((day) => day.date === date) || null,
    [scheduleDays, date]
  );

  const slotOptions = useMemo(
    () => (daySchedule?.creneaux || []).map((slot) => ({
      value: slot.horairePublicId,
      label: [
        slot.heureCelebration ? formatParishTimeInUserZone(slot.heureCelebration) : 'Heure non définie',
        slot.libelle,
        slot.programmeJourOverride ? 'personnalisé' : null,
        slot.dateSpecifique && !slot.programmeJourOverride ? 'ponctuel' : null,
        slot.uniqueSurParoisse ? 'messe unique' : null,
      ].filter(Boolean).join(' · '),
    })),
    [daySchedule]
  );

  const isPastDay = Boolean(date && date < todayInParishZone());
  const programmeMode = daySchedule?.modeProgramme || 'HEBDOMADAIRE';
  const followsWeeklyDefault = programmeMode === 'HEBDOMADAIRE';

  const openEdit = (item) => {
    setError(null);
    setInfo(null);
    setEditing(item);
    setSelectedSlot(item.horairePublicId || slotOptions[0]?.value || '');
  };

  const closeEdit = () => {
    if (saving) return;
    setEditing(null);
    setSelectedSlot('');
  };

  const saveSchedule = async () => {
    if (!editing || !parishId || !selectedSlot) {
      setError('Choisissez un créneau avant d’enregistrer.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      await dashboardService.updateCelebrationSchedule(
        parishId,
        editing.demandeDatePublicId,
        selectedSlot
      );
      setEditing(null);
      setSelectedSlot('');
      await invalidate(parishId);
      await refetch();
      setInfo('Le créneau de célébration a été mis à jour. La date de la demande reste inchangée.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de modifier la programmation');
    } finally {
      setSaving(false);
    }
  };

  const openDayScheduleEditor = () => {
    setError(null);
    setInfo(null);
    setDaySlotsDraft(draftFromSchedule(daySchedule));
    setEditingDaySchedule(true);
  };

  const changeDaySlot = (index, key, value) => {
    setDaySlotsDraft((current) => current.map((slot, i) => (
      i === index ? { ...slot, [key]: value } : slot
    )));
  };

  const addDaySlot = () => {
    setDaySlotsDraft((current) => [
      ...current,
      { heureCelebration: '', libelle: '', natureHonoraire: null },
    ]);
  };

  const removeDaySlot = (index) => {
    setDaySlotsDraft((current) => current.filter((_, i) => i !== index));
  };

  const invalidateScheduleQueries = async () => {
    if (!parishId) return;
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: parishProgrammeKeys.all(parishId) }),
      queryClient.invalidateQueries({ queryKey: qk.horairesParish(parishId) }),
      queryClient.invalidateQueries({ queryKey: qk.horairesPublicActives }),
    ]);
  };

  const saveDayProgramme = async () => {
    if (!parishId || !date) return;
    const cleaned = daySlotsDraft
      .map((slot) => ({
        heureCelebration: slot.heureCelebration,
        libelle: slot.libelle?.trim() || null,
        natureHonoraire: slot.natureHonoraire || null,
      }))
      .filter((slot) => slot.heureCelebration);

    if (!cleaned.length) {
      setError('Ajoutez au moins un créneau pour personnaliser cette journée.');
      return;
    }
    if (cleaned.length !== daySlotsDraft.length) {
      setError('Chaque créneau doit avoir une heure.');
      return;
    }
    const times = cleaned.map((slot) => slot.heureCelebration);
    if (new Set(times).size !== times.length) {
      setError('Deux créneaux ne peuvent pas avoir la même heure.');
      return;
    }

    try {
      setSavingDaySchedule(true);
      setError(null);
      await scheduleService.updateProgrammeForDate(parishId, date, cleaned);
      setEditingDaySchedule(false);
      await invalidateScheduleQueries();
      await refetchSchedule();
      setInfo(
        'Programme de la journée personnalisé. Les demandes déjà enregistrées ne sont pas déplacées automatiquement.'
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de modifier les créneaux de la journée');
    } finally {
      setSavingDaySchedule(false);
    }
  };

  const resetDayProgramme = async () => {
    if (!parishId || !date) return;
    try {
      setSavingDaySchedule(true);
      setError(null);
      await scheduleService.resetProgrammeForDate(parishId, date);
      setConfirmResetSchedule(false);
      await invalidateScheduleQueries();
      await refetchSchedule();
      setInfo('La journée suit de nouveau les créneaux hebdomadaires par défaut.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Impossible de revenir au programme par défaut');
    } finally {
      setSavingDaySchedule(false);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        title={`Programmations du ${longDate(date)}`}
        subtitle={isPastDay
          ? 'Historique de la journée — les célébrations passées sont conservées en lecture seule.'
          : 'Pilotez les intentions enregistrées et le programme disponible pour cette date.'}
        actions={(
          <Link className="btn btn-secondary" to={ROUTES.DASHBOARD}>
            Retour au dashboard
          </Link>
        )}
      />

      {queryError ? (
        <div className="alert-danger" role="alert">
          {queryError.message || 'Impossible de charger les programmations de la journée.'}
        </div>
      ) : null}
      {error ? <div className="alert-danger" role="alert">{error}</div> : null}
      {info ? <div className="alert-success" role="status">{info}</div> : null}

      <AppCard
        title={`Célébrations de la journée${celebrations.length ? ` (${celebrations.length})` : ''}`}
        subtitle={isPastDay
          ? 'Les éléments passés restent visibles pour la traçabilité.'
          : 'Ici vous modifiez le créneau d’une demande précise, sans changer sa date.'}
      >
        {isLoading ? <p className="muted">Chargement des programmations…</p> : null}
        {!isLoading && !queryError ? (
          <div className="table-card">
            <table className="app-table">
              <thead>
                <tr>
                  <th>Heure</th>
                  <th>Demande</th>
                  <th>Intention</th>
                  <th>Fidèle</th>
                  <th>Statuts</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {celebrations.map((item) => (
                  <tr key={item.demandeDatePublicId}>
                    <td data-label="Heure">
                      <strong>
                        {item.heureCelebration
                          ? formatParishTimeInUserZone(item.heureCelebration)
                          : '—'}
                      </strong>
                      {item.horaireLibelle ? (
                        <span className="muted" style={{ display: 'block', fontSize: '0.82em' }}>
                          {item.horaireLibelle}
                        </span>
                      ) : null}
                    </td>
                    <td data-label="Demande">
                      <Link to={`/admin/demandes/${item.demandePublicId}`}>
                        {item.codeSuivie || 'Voir la demande'}
                      </Link>
                      <span className="muted" style={{ display: 'block', fontSize: '0.82em' }}>
                        {item.typeDemandeLibelle || '—'}
                      </span>
                    </td>
                    <td data-label="Intention">{item.intention || '—'}</td>
                    <td data-label="Fidèle">{item.fidele || '—'}</td>
                    <td data-label="Statuts">
                      <div className="dashboard-upcoming-statuses">
                        <AppBadge value={item.statutDemande} />
                        <AppBadge value={item.statutPaiement} />
                        {item.celebre ? <span className="badge badge-success">Célébrée</span> : null}
                      </div>
                    </td>
                    <td data-label="Action">
                      {canEdit && item.modifiable ? (
                        <AppButton
                          size="sm"
                          variant="secondary"
                          onClick={() => openEdit(item)}
                        >
                          Modifier le créneau
                        </AppButton>
                      ) : (
                        <span className="muted">
                          {item.celebre || isPastDay ? 'Historique' : 'Lecture seule'}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
                {!celebrations.length ? (
                  <tr>
                    <td colSpan={6} className="muted" data-label="">
                      Aucune demande programmée pour cette journée.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        ) : null}
      </AppCard>

      <AppCard
        title="Créneaux disponibles ce jour"
        subtitle="Cette grille détermine les horaires proposés pour cette date."
      >
        {scheduleLoading ? <p className="muted">Chargement des créneaux…</p> : null}
        {scheduleError ? (
          <p className="text-red-600">{scheduleError.message || 'Impossible de charger les créneaux.'}</p>
        ) : null}
        {!scheduleLoading && !scheduleError ? (
          <div className="stack" style={{ gap: 14 }}>
            <div>
              <strong>{PROGRAMME_MODE_LABELS[programmeMode] || programmeMode}</strong>
              {followsWeeklyDefault ? (
                <p className="muted" style={{ margin: '4px 0 0' }}>
                  Aucune exception n’est appliquée : cette date reprend automatiquement les horaires habituels de ce jour de semaine.
                </p>
              ) : (
                <p className="muted" style={{ margin: '4px 0 0' }}>
                  Cette date possède une exception au programme hebdomadaire.
                </p>
              )}
            </div>

            {slotOptions.length ? (
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {slotOptions.map((slot) => <li key={slot.value}>{slot.label}</li>)}
              </ul>
            ) : (
              <p className="muted" style={{ margin: 0 }}>
                Aucun créneau paroissial n’est disponible pour cette journée.
              </p>
            )}

            {canManageSchedule && !isPastDay ? (
              <div className="button-row">
                <AppButton variant="secondary" onClick={openDayScheduleEditor}>
                  Modifier les créneaux du jour
                </AppButton>
                {!followsWeeklyDefault ? (
                  <AppButton variant="secondary" onClick={() => setConfirmResetSchedule(true)}>
                    Revenir au programme par défaut
                  </AppButton>
                ) : null}
              </div>
            ) : null}

            {!isPastDay ? (
              <p className="muted" style={{ margin: 0, fontSize: '0.88rem' }}>
                Modifier la grille du jour n’altère pas automatiquement les demandes déjà enregistrées ;
                leurs créneaux peuvent être ajustés individuellement dans le tableau ci-dessus.
              </p>
            ) : null}
          </div>
        ) : null}
      </AppCard>

      <AppDialog
        open={Boolean(editing)}
        title="Modifier le créneau de la célébration"
        confirmLabel={saving ? 'Enregistrement…' : 'Enregistrer'}
        cancelLabel="Annuler"
        busy={saving}
        onCancel={closeEdit}
        onConfirm={saveSchedule}
      >
        {editing ? (
          <div className="stack" style={{ gap: 12 }}>
            <p style={{ margin: 0 }}>
              <strong>{editing.codeSuivie}</strong> — {editing.intention || 'Intention sans libellé'}
            </p>
            <p className="muted" style={{ margin: 0 }}>
              La date du {longDate(editing.dateCelebration)} est conservée. Seul le créneau peut changer.
            </p>
            <div className="form-field">
              <label htmlFor="daily-programme-slot">Nouveau créneau</label>
              <AppSelect
                id="daily-programme-slot"
                value={selectedSlot}
                onChange={setSelectedSlot}
                options={slotOptions}
                placeholder="Choisir un créneau"
                disabled={saving || !slotOptions.length}
                required
              />
            </div>
          </div>
        ) : null}
      </AppDialog>

      <AppDialog
        open={editingDaySchedule}
        title={`Modifier les créneaux du ${longDate(date)}`}
        confirmLabel={savingDaySchedule ? 'Enregistrement…' : 'Enregistrer le programme'}
        cancelLabel="Annuler"
        busy={savingDaySchedule}
        size="lg"
        onCancel={() => {
          if (!savingDaySchedule) setEditingDaySchedule(false);
        }}
        onConfirm={saveDayProgramme}
      >
        <div className="stack" style={{ gap: 14 }}>
          <p className="muted" style={{ margin: 0 }}>
            Les créneaux ci-dessous remplaceront les horaires hebdomadaires uniquement pour cette date.
          </p>
          {daySlotsDraft.map((slot, index) => (
            <div className="form-grid" key={`${index}-${slot.heureCelebration}`}>
              <div className="form-field">
                <label htmlFor={`day-slot-time-${index}`}>Heure *</label>
                <AppInput
                  id={`day-slot-time-${index}`}
                  type="time"
                  value={slot.heureCelebration}
                  onChange={(e) => changeDaySlot(index, 'heureCelebration', e.target.value)}
                  disabled={savingDaySchedule}
                />
              </div>
              <div className="form-field">
                <label htmlFor={`day-slot-label-${index}`}>Libellé</label>
                <AppInput
                  id={`day-slot-label-${index}`}
                  value={slot.libelle}
                  maxLength={150}
                  placeholder="Ex. Messe du matin"
                  onChange={(e) => changeDaySlot(index, 'libelle', e.target.value)}
                  disabled={savingDaySchedule}
                />
              </div>
              <div className="form-field full">
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => removeDaySlot(index)}
                  disabled={savingDaySchedule || daySlotsDraft.length === 1}
                >
                  Retirer ce créneau
                </AppButton>
              </div>
            </div>
          ))}
          <div className="button-row">
            <AppButton
              type="button"
              variant="secondary"
              onClick={addDaySlot}
              disabled={savingDaySchedule}
            >
              + Ajouter un créneau
            </AppButton>
          </div>
        </div>
      </AppDialog>

      <AppDialog
        open={confirmResetSchedule}
        title="Revenir au programme hebdomadaire"
        confirmLabel={savingDaySchedule ? 'Réinitialisation…' : 'Réappliquer le programme par défaut'}
        cancelLabel="Annuler"
        busy={savingDaySchedule}
        onCancel={() => {
          if (!savingDaySchedule) setConfirmResetSchedule(false);
        }}
        onConfirm={resetDayProgramme}
      >
        <p style={{ margin: 0 }}>
          Les créneaux spécifiques du {longDate(date)} seront retirés. Cette date reprendra les horaires hebdomadaires habituels.
          Les demandes déjà enregistrées conserveront leur créneau tant que vous ne les modifiez pas individuellement.
        </p>
      </AppDialog>
    </div>
  );
}
