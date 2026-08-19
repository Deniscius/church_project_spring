import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DEFAULT_FIDELE_NAME } from '../../utils/personName';

const GUIDE_STORAGE_KEY = 'demande_user_guide_open_v1';

const STEPS = [
  {
    id: 1,
    title: 'Votre intention',
    points: [
      'Écrivez simplement pour qui ou pour quoi la messe est demandée.',
      'Indiquez un téléphone valide : il servira aussi à retrouver toutes vos demandes.',
      `Le prénom et le nom restent facultatifs ; sans eux, le dossier affiche « ${DEFAULT_FIDELE_NAME} ».`,
    ],
  },
  {
    id: 2,
    title: 'Célébration',
    points: [
      'Choisissez la paroisse, la formule, la date puis l’horaire.',
      'Si le tarif dépend du jour choisi, Missanye l’ajuste automatiquement et vous le signale avant de continuer.',
    ],
  },
  {
    id: 3,
    title: 'Paiement',
    points: [
      'Choisissez votre mode de paiement et vérifiez le résumé.',
      'Confirmez pour obtenir immédiatement votre code de suivi.',
    ],
  },
];

/** Guide d'aide facultatif : le parcours principal reste visible sans texte supplémentaire. */
export default function DemandeUserGuide({ step = 1 }) {
  const [open, setOpen] = useState(() => {
    try {
      const raw = sessionStorage.getItem(GUIDE_STORAGE_KEY);
      if (raw === '1') return true;
      if (raw === '0') return false;
    } catch {
      /* ignore */
    }
    return false;
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
    <aside className={`demande-user-guide${open ? ' is-open' : ''}`} aria-label="Aide pour la demande">
      <button
        type="button"
        className="demande-user-guide-toggle"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <span>
          <strong>Besoin d’aide ?</strong>
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
            Après le dépôt, retrouvez toutes les demandes liées à votre{' '}
            <Link to="/suivi">numéro de téléphone</Link> ou ouvrez directement une demande avec son code de suivi.
          </p>
        </div>
      ) : null}
    </aside>
  );
}
