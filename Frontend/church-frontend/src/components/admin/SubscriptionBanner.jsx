import React from 'react';
import { useTenant } from '../../hooks/useTenant';

// Fenêtre d'alerte alignée sur platform.subscription-warning-days côté serveur.
const WARNING_DAYS = 15;

function daysUntil(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return Math.ceil((date.getTime() - Date.now()) / 86_400_000);
}

/**
 * La suspension d'une paroisse bloque la connexion de toute son équipe : elle
 * ne doit jamais arriver en surprise.
 */
export default function SubscriptionBanner() {
  const { activeParish } = useTenant();
  const expiresAt = activeParish?.subscriptionExpiresAt;
  const remaining = daysUntil(expiresAt);

  if (remaining === null || remaining > WARNING_DAYS) {
    return null;
  }

  const echeance = new Date(expiresAt).toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });

  if (remaining < 0) {
    return (
      <div className="alert-error subscription-banner" role="alert">
        <strong>Abonnement échu depuis le {echeance}.</strong> L’accès de la paroisse sera
        suspendu à l’issue de la période de tolérance. Contactez le comptable de la
        plateforme pour régulariser.
      </div>
    );
  }

  return (
    <div className="alert-info subscription-banner" role="status">
      <strong>
        Abonnement à renouveler {remaining === 0 ? 'aujourd’hui' : `dans ${remaining} jour${remaining > 1 ? 's' : ''}`}
      </strong>{' '}
      — échéance le {echeance}. Rapprochez-vous du comptable de la plateforme.
    </div>
  );
}
