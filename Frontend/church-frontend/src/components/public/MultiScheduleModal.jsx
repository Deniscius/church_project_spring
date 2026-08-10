import React, { useMemo } from 'react';
import { createPortal } from 'react-dom';
import AppButton from '../ui/AppButton';
import AppSelect from '../ui/AppSelect';
import AppInput from '../ui/AppInput';
import { WEEK_DAY_LABELS } from '../../constants/enums';
import { emptySchedule, getDayEnumFromDateString, resolveHorairesForDate } from '../../utils/schedulingUtils';
import { formatParishTimeInUserZone } from '../../utils/formatTime';

function formatFrDate(iso) {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
  });
}

/**
 * Modal convivial : horaires de la série (triduum / neuvaine / trentaine).
 * Trentaine : focus sur les dimanches à ajuster.
 */
export default function MultiScheduleModal({
  open,
  onClose,
  dates = [],
  horaires = [],
  dateSchedules = {},
  onChangeSchedule,
  onApplyDefaultToAll,
  heurePersonnalise = false,
  trentaine = false,
  title = 'Horaires de célébration',
}) {
  const sundayDates = useMemo(
    () => dates.filter((iso) => getDayEnumFromDateString(iso) === 'DIMANCHE'),
    [dates]
  );

  const visibleDates = trentaine ? sundayDates : dates;

  if (!open) return null;

  const panel = (
    <div className="dialog-backdrop" onClick={onClose}>
      <div
        className="dialog-panel dialog-panel--lg multi-schedule-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="multi-schedule-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="formule-modal-head">
          <div>
            <p className="formule-modal-kicker">Série de célébrations</p>
            <h2 id="multi-schedule-title" className="dialog-title" style={{ margin: 0 }}>
              {title}
            </h2>
            <p className="muted" style={{ margin: '6px 0 0' }}>
              {trentaine
                ? `${dates.length} jours générés automatiquement. Ajustez uniquement les dimanches si besoin.`
                : `Choisissez l’horaire de chaque jour — ${dates.length} célébrations consécutives.`}
            </p>
          </div>
          <button type="button" className="formule-modal-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </header>

        {!trentaine && dates.length > 0 ? (
          <div className="button-row" style={{ marginBottom: 12 }}>
            <AppButton type="button" variant="secondary" onClick={onApplyDefaultToAll}>
              Appliquer le 1er créneau disponible à tous
            </AppButton>
          </div>
        ) : null}

        {trentaine && sundayDates.length === 0 ? (
          <p className="muted">Aucun dimanche dans cette trentaine — rien à ajuster.</p>
        ) : null}

        <div className="multi-schedule-grid">
          {visibleDates.map((iso) => {
            const day = getDayEnumFromDateString(iso);
            const dayHoraires = resolveHorairesForDate(horaires, iso);
            const schedule = dateSchedules[iso] || emptySchedule();
            const selectedOk = dayHoraires.some((h) => h.publicId === schedule.horairePublicId);
            const options = dayHoraires.map((h) => ({
              value: h.publicId,
              label: [formatParishTimeInUserZone(h.heureCelebration), h.libelle].filter(Boolean).join(' · '),
            }));
            const fullIndex = dates.indexOf(iso);

            return (
              <article
                key={iso}
                className={`multi-schedule-card${day === 'DIMANCHE' ? ' is-sunday' : ''}`}
              >
                <header className="multi-schedule-card-head">
                  <span className="multi-schedule-day">
                    {trentaine ? 'Dimanche' : `Jour ${fullIndex + 1}`}
                  </span>
                  <strong>{formatFrDate(iso)}</strong>
                  <span className="muted">{WEEK_DAY_LABELS[day] || day}</span>
                </header>
                <div className="form-field" style={{ margin: 0 }}>
                  <label htmlFor={`ms-${iso}`}>Horaire</label>
                  <AppSelect
                    id={`ms-${iso}`}
                    value={selectedOk ? schedule.horairePublicId : ''}
                    placeholder="— Choisir —"
                    options={options}
                    onChange={(id) => {
                      const h = dayHoraires.find((x) => x.publicId === id);
                      onChangeSchedule?.(iso, {
                        horairePublicId: id,
                        horaireLibelle: h
                          ? `${h.heureCelebration || ''} ${h.libelle || ''}`.trim()
                          : '',
                        heureCelebration: h?.heureCelebration || '',
                        jourSemaine: h?.jourSemaine || day,
                      });
                    }}
                  />
                  {options.length === 0 ? (
                    <small className="text-red-600">Aucun créneau ce jour-là.</small>
                  ) : null}
                </div>
                {heurePersonnalise ? (
                  <div className="form-field" style={{ margin: 0 }}>
                    <label htmlFor={`ms-perso-${iso}`}>Heure personnalisée</label>
                    <AppInput
                      id={`ms-perso-${iso}`}
                      type="time"
                      value={schedule.heurePersonnalisee || ''}
                      onChange={(e) => onChangeSchedule?.(iso, { heurePersonnalisee: e.target.value })}
                    />
                  </div>
                ) : null}
              </article>
            );
          })}
        </div>

        {trentaine ? (
          <p className="muted text-sm" style={{ marginTop: 12 }}>
            Les autres jours de la trentaine gardent automatiquement le premier créneau du jour
            correspondant dans le programme paroissial.
          </p>
        ) : null}

        <div className="dialog-actions">
          <AppButton type="button" onClick={onClose}>
            Valider les horaires
          </AppButton>
        </div>
      </div>
    </div>
  );

  return createPortal(panel, document.body);
}
