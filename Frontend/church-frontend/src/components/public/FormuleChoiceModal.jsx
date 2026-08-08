import React, { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import AppButton from '../ui/AppButton';
import {
  getForfaitDureeLabel,
  isMultiCelebrationForfait,
  NATURE_FORFAIT_OPTIONS,
} from '../../constants/enums';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatAllowedDays } from '../../utils/schedulingUtils';

/**
 * Modal de choix de formule (messe unique / triduum / neuvaine…) — cartes visuelles.
 */
export default function FormuleChoiceModal({
  open,
  onClose,
  types = [],
  typesLoading = false,
  forfaits = [],
  forfaitsLoading = false,
  selectedTypeId = '',
  selectedNature = '',
  onSelectType,
  onSelectForfait,
}) {
  const [step, setStep] = useState('type'); // type | forfait

  useEffect(() => {
    if (!open) return;
    setStep(selectedTypeId ? 'forfait' : 'type');
  }, [open, selectedTypeId]);

  useEffect(() => {
    if (!open) return undefined;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener('keydown', onKey);
    };
  }, [open, onClose]);

  const typeCards = useMemo(
    () => (types || []).map((t) => {
      const lib = (t.libelle || '').toLowerCase();
      let dureeHint = '';
      if (lib.includes('triduum')) dureeHint = '3 célébrations';
      else if (lib.includes('neuvaine')) dureeHint = '9 célébrations';
      else if (lib.includes('trentaine')) dureeHint = '30 célébrations';
      else dureeHint = 'Formule paroissiale';
      return {
        id: t.publicId,
        title: t.libelle || 'Formule',
        dureeHint,
        delai: t.delaiMinimumHeures ?? 24,
        days: t.joursCelebrationAutorises || [],
        raw: t,
      };
    }),
    [types]
  );

  const forfaitCards = useMemo(
    () => NATURE_FORFAIT_OPTIONS
      .map((opt) => {
        const f = (forfaits || []).find((x) => x.natureForfait === opt.value);
        if (!f) return null;
        const n = f.nombreCelebration != null ? Number(f.nombreCelebration) : 1;
        const multi = isMultiCelebrationForfait(n);
        return {
          nature: opt.value,
          title: opt.label,
          hint: opt.hint,
          duree: getForfaitDureeLabel(n),
          jours: n,
          multi,
          montant: f.montantForfait != null ? Number(f.montantForfait) : 0,
          forfait: f,
        };
      })
      .filter(Boolean),
    [forfaits]
  );

  if (!open) return null;

  const panel = (
    <div className="dialog-backdrop formule-modal-backdrop" onClick={onClose}>
      <div
        className="dialog-panel dialog-panel--lg formule-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="formule-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="formule-modal-head">
          <div>
            <p className="formule-modal-kicker">Choix de formule</p>
            <h2 id="formule-modal-title" className="dialog-title" style={{ margin: 0 }}>
              {step === 'type' ? 'Quelle célébration ?' : 'Quel tarif ?'}
            </h2>
            <p className="muted" style={{ margin: '6px 0 0' }}>
              {step === 'type'
                ? 'Triduum, neuvaine ou messe unique — voyez clairement la durée avant de valider.'
                : 'Comparez les natures et le montant en un coup d’œil.'}
            </p>
          </div>
          <button type="button" className="formule-modal-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </header>

        {step === 'type' ? (
          <div className="formule-card-grid">
            {typesLoading ? <p className="muted">Chargement des formules…</p> : null}
            {!typesLoading && typeCards.length === 0 ? (
              <p className="muted">Aucune formule configurée pour cette paroisse.</p>
            ) : null}
            {typeCards.map((card) => (
              <button
                key={card.id}
                type="button"
                className={`formule-choice-card${selectedTypeId === card.id ? ' is-selected' : ''}`}
                onClick={() => {
                  onSelectType?.(card.raw);
                  setStep('forfait');
                }}
              >
                <span className="formule-choice-badge">{card.dureeHint}</span>
                <strong className="formule-choice-title">{card.title}</strong>
                <span className="formule-choice-meta">
                  Délai min. {card.delai} h
                  {card.days.length ? ` · ${formatAllowedDays(card.days)}` : ''}
                </span>
              </button>
            ))}
          </div>
        ) : (
          <div className="formule-card-grid">
            <button
              type="button"
              className="formule-back-link"
              onClick={() => setStep('type')}
            >
              ← Changer de formule
            </button>
            {forfaitsLoading ? <p className="muted">Chargement des tarifs…</p> : null}
            {!forfaitsLoading && forfaitCards.length === 0 ? (
              <p className="text-red-600">Aucun tarif actif pour cette formule.</p>
            ) : null}
            {forfaitCards.map((card) => (
              <button
                key={card.nature}
                type="button"
                className={`formule-choice-card${selectedNature === card.nature ? ' is-selected' : ''}`}
                onClick={() => {
                  onSelectForfait?.(card.forfait);
                  onClose?.();
                }}
              >
                <span className="formule-choice-badge">
                  {card.multi ? card.duree : '1 célébration'}
                </span>
                <strong className="formule-choice-title">{card.title}</strong>
                <span className="formule-choice-price">{formatCurrency(card.montant)}</span>
                <span className="formule-choice-meta">{card.hint}</span>
                {card.multi ? (
                  <span className="formule-choice-days">
                    {card.jours} jours de célébration
                  </span>
                ) : null}
              </button>
            ))}
          </div>
        )}

        <div className="dialog-actions">
          <AppButton variant="secondary" onClick={onClose}>
            Fermer
          </AppButton>
        </div>
      </div>
    </div>
  );

  return createPortal(panel, document.body);
}
