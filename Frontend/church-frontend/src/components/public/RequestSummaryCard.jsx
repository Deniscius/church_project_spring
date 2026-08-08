import React from 'react';
import AppCard from '../ui/AppCard';
import { getForfaitDureeLabel, isMultiCelebrationForfait } from '../../constants/enums';
import { usePublicDemandeDraft } from '../../contexts/publicDemandeDraft.context';
import { formatCurrency } from '../../utils/formatCurrency';
import { DEFAULT_FIDELE_NAME } from '../../utils/personName';

/**
 * Résumé limité à l'étape courante (et aux étapes déjà validées).
 * Les champs des étapes à venir ne sont pas affichés.
 */
export default function RequestSummaryCard({ step = 4, mode = 'wizard' }) {
  const { draft } = usePublicDemandeDraft();
  const multi = isMultiCelebrationForfait(draft.forfaitNombreCelebration);
  const dates = multi
    ? (draft.datesCelebration || []).filter(Boolean).sort()
    : (draft.dateDebut ? [draft.dateDebut] : []);

  const showThrough = mode === 'full' ? 4 : step;
  const rows = [];

  if (draft.prefillFromSchedule && showThrough >= 2) {
    rows.push([
      'Prérempli',
      [
        draft.paroisseNom,
        draft.horaireLibelle || draft.horaireHeureCelebration,
        draft.dateDebut,
      ].filter(Boolean).join(' · ') || 'Créneau depuis l’accueil',
    ]);
  }

  if (showThrough >= 1) {
    rows.push(
      ['Intention de messe', draft.intention?.trim() || '—'],
      [
        'Demandeur',
        [draft.prenomFidele, draft.nomFidele].filter(Boolean).join(' ')
          || `${DEFAULT_FIDELE_NAME} (si non renseigné)`,
      ],
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
    );
  }

  if (showThrough >= 3) {
    rows.push(
      [
        multi
          ? `Dates (${getForfaitDureeLabel(draft.forfaitNombreCelebration)})`
          : 'Date de célébration',
        dates.length === 0
          ? '—'
          : multi
            ? `${dates[0]} → ${dates[dates.length - 1]} (${dates.length} jours)`
            : dates[0],
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
                return h ? `${iso.slice(5)} ${String(h).slice(0, 5)}` : null;
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

  if (showThrough >= 4) {
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
    </AppCard>
  );
}
