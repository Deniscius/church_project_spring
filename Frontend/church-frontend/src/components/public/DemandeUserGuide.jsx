import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DEFAULT_FIDELE_NAME } from '../../utils/personName';

const GUIDE_STORAGE_KEY = 'demande_user_guide_open_v1';

const STEPS = [
  {
    id: 1,
    title: 'Intention',
    points: [
      'Indiquez pour qui ou pour quoi vous demandez la messe.',
      `Sans prénom ni nom, le demandeur sera affiché comme « ${DEFAULT_FIDELE_NAME} ».`,
      'Le téléphone est obligatoire pour retrouver vos demandes dans Suivi.',
    ],
  },
  {
    id: 2,
    title: 'Lieu & date',
    points: [
      'Choisissez la paroisse, la formule, puis la date et l’horaire.',
      'Depuis un créneau des horaires, paroisse, formule, date et heure sont figées.',
    ],
  },
  {
    id: 3,
    title: 'Paiement',
    points: [
      'Choisissez le mode de paiement, puis confirmez pour obtenir votre code de suivi.',
      'Mémorisez aussi le téléphone saisi pour le dépôt.',
    ],
  },
];

/**
 * Guide collapsible du parcours « Déposer une intention ».
 * Ouvert par défaut à l’étape 1 ; l’état ouvert/fermé est mémorisé en session.
 */
export default function DemandeUserGuide({ step = 1 }) {
  const [open, setOpen] = useState(() => {
    try {
      const raw = sessionStorage.getItem(GUIDE_STORAGE_KEY);
      if (raw === '0') return false;
      if (raw === '1') return true;
    } catch {
      /* ignore */
    }
    return step === 1;
  });

  useEffect(() => {
    try {
      sessionStorage.setItem(GUIDE_STORAGE_KEY, open ? '1' : '0');
    } catch {
      /* ignore */
    }
  }, [open]);

  const current = STEPS.find((s) => s.id === step) || STEPS[0];

  return (
    <aside className={`demande-user-guide${open ? ' is-open' : ''}`} aria-label="Guide de la demande">
      <button
        type="button"
        className="demande-user-guide-toggle"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <span>
          <strong>Guide</strong>
          <span className="muted"> — étape {step}/3 · {current.title}</span>
        </span>
        <span className="demande-user-guide-chevron" aria-hidden="true">
          {open ? '−' : '+'}
        </span>
      </button>

      {open ? (
        <div className="demande-user-guide-body">
          <ol className="demande-user-guide-steps">
            {STEPS.map((s) => (
              <li
                key={s.id}
                className={
                  s.id === step
                    ? 'is-current'
                    : s.id < step
                      ? 'is-done'
                      : undefined
                }
              >
                <span className="demande-user-guide-index">{s.id}</span>
                <div>
                  <strong>{s.title}</strong>
                  {s.id === step ? (
                    <ul>
                      {s.points.map((p) => (
                        <li key={p}>{p}</li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </li>
            ))}
          </ol>

          <p className="demande-user-guide-foot muted">
            Après dépôt, retrouvez vos demandes avec le{' '}
            <strong>code de suivi</strong> ou le{' '}
            <Link to="/suivi">numéro de téléphone</Link> saisi ici.
          </p>
        </div>
      ) : null}
    </aside>
  );
}
