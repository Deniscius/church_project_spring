import React from 'react';
import AppCard from '../ui/AppCard';
import { getForfaitDureeLabel, isMultiCelebrationForfait } from '../../constants/enums';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDateShort } from '../../utils/formatDate';
import { DEFAULT_FIDELE_NAME, formatFideleName } from '../../utils/personName';

/**
 * Résumé limité à l'étape courante (et aux étapes déjà validées).
 * Les champs des étapes à venir ne sont pas affichés.
 */
export default function RequestSummaryCard({ step = 3, mode = 'wizard' }) {
  const { draft } = usePublicDemandeDraft();
  const multi = isMultiCelebrationForfait(draft.forfaitNombreCelebration);
  const dates = multi
    ? (draft.datesCelebration || []).filter(Boolean).sort()
    : (draft.dateDebut ? [draft.dateDebut] : []);

  // Wizard 3 étapes : 1 intention · 2 lieu+date · 3 paiement
  const showThrough = mode === 'full' ? 3 : step;
  const rows = [];
  const demandeur = formatFideleName(draft.prenomFidele, draft.nomFidele);
  const anonymousDemandeur = demandeur === DEFAULT_FIDELE_NAME;

  if (draft.prefillFromSchedule && showThrough >= 1) {
    rows.push([
      'Créneau',
      [
        draft.paroisseNom,
        draft.horaireLibelle || draft.horaireHeureCelebration,
        formatDateShort(draft.dateDebut),
      ].filter(Boolean).join(' · ') || 'Depuis les horaires',
    ]);
  }

  if (showThrough >= 1) {
    rows.push(
      ['Intention de messe', draft.intention?.trim() || '—'],
      ['Demandeur', demandeur],
    );
  }

  if (showThrough >= 2) {
    rows.push(
      ['Paroisse', draft.paroisseNom || '—'],
      ['Type', draft.typeDemandeLibelle || '—'],
      ['Forfait', draft.forfaitLabel || '—'],
      [
        'Montant',
        draft.forfaitMontant != null
          ? formatCurrency(Number(draft.forfaitMontant))
          : '—',
      ],
      [
        multi
          ? `Dates (${getForfaitDureeLabel(draft.forfaitNombreCelebration)})`
          : 'Date de célébration',
        dates.length === 0
          ? '—'
          : multi
            ? `${formatDateShort(dates[0])} → ${formatDateShort(dates[dates.length - 1])} (${dates.length} jours)`
            : formatDateShort(dates[0]),
      ],
      [
        'Horaire',
        multi
          ? (() => {
            const schedules = draft.dateSchedules || {};
            const bits = dates
              .map((iso) => {
                const s = schedules[iso] || {};
                const h = s.heurePersonnalisee || s.heureCelebration?.slice?.(0, 5) || s.horaireLibelle;
                return h ? `${formatDateShort(iso)} ${String(h).slice(0, 5)}` : null;
              })
              .filter(Boolean);
            return bits.length ? bits.join(' · ') : '—';
          })()
          : `${draft.horaireLibelle || '—'}${
            draft.forfaitHeurePersonnalise && draft.heurePersonnalisee
              ? ` · perso ${draft.heurePersonnalisee}`
              : ''
          }`,
      ],
    );
  }

  if (showThrough >= 3) {
    rows.push(['Paiement', draft.typePaiementLibelle || '—']);
  }

  return (
    <AppCard
      title="Résumé"
      subtitle={
        mode === 'full'
          ? 'Aperçu du dossier avant envoi.'
          : `Étape ${step} — uniquement ce qui est déjà saisi.`
      }
    >
      <div className="info-list">
        {rows.map(([label, value]) => (
          <div className="info-row" key={label}>
            <span>{label}</span>
            <span>{value}</span>
          </div>
        ))}
      </div>
      {showThrough >= 1 && anonymousDemandeur ? (
        <p className="muted demande-summary-hint">
          Sans nom saisi, le dossier porte « {DEFAULT_FIDELE_NAME} ».
        </p>
      ) : null}
    </AppCard>
  );
}
