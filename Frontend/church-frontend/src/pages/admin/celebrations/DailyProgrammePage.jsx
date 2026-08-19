import React, { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import AppButton from '../../../components/ui/AppButton';
import AppDialog from '../../../components/ui/AppDialog';
import AppSelect from '../../../components/ui/AppSelect';
import { useTenant } from '../../../hooks/useTenant';
import { usePermissions } from '../../../hooks/usePermissions';
import { PERMISSIONS } from '../../../constants/roles';
import { ROUTES } from '../../../constants/routes';
import { formatParishTimeInUserZone } from '../../../utils/formatTime';
import { dashboardService } from '../../../services/dashboard.service';
import { useParishProgrammeQuery } from '../../../hooks/queries/useParishProgramme';
import {
  useInvalidateParishCelebrations,
  useParishCelebrationsForDay,
} from '../../../hooks/queries/useParishUpcomingCelebrations';

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

export default function DailyProgrammePage() {
  const { date } = useParams();
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const canEdit = has(PERMISSIONS.DEMAND_EDIT);
  const invalidate = useInvalidateParishCelebrations();
  const [editing, setEditing] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState('');
  const [saving, setSaving] = useState(false);
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
        slot.dateSpecifique ? 'ponctuelle' : null,
        slot.uniqueSurParoisse ? 'messe unique' : null,
      ].filter(Boolean).join(' · '),
    })),
    [daySchedule]
  );

  const isPastDay = Boolean(date && date < todayInParishZone());

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

  return (
    <div className="stack">
      <PageHeader
        title={`Programmations du ${longDate(date)}`}
        subtitle={isPastDay
          ? 'Historique de la journée — les célébrations passées sont conservées en lecture seule.'
          : 'Pilotez les intentions réellement programmées et ajustez leur créneau si nécessaire.'}
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
          : 'La modification change uniquement le créneau de cette journée, jamais la date.'}
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
        subtitle="Ces horaires viennent du programme paroissial résolu pour cette date."
      >
        {scheduleLoading ? <p className="muted">Chargement des créneaux…</p> : null}
        {scheduleError ? (
          <p className="text-red-600">{scheduleError.message || 'Impossible de charger les créneaux.'}</p>
        ) : null}
        {!scheduleLoading && !scheduleError ? (
          slotOptions.length ? (
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              {slotOptions.map((slot) => <li key={slot.value}>{slot.label}</li>)}
            </ul>
          ) : (
            <p className="muted" style={{ margin: 0 }}>
              Aucun créneau paroissial n’est disponible pour cette journée.
            </p>
          )
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
    </div>
  );
}
