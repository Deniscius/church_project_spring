import React from 'react';
import AppCard from '../ui/AppCard';

export default function TrackingCodeForm() {
  return (
    <AppCard title="Comment ça marche ?" subtitle="Sans créer de compte.">
      <ul className="help-list">
        <li>Après le dépôt, vous recevez un code de suivi unique.</li>
        <li>Conservez-le ou copiez-le : il sert à suivre et à payer votre demande.</li>
        <li>La casse n’importe pas : minuscules ou majuscules sont acceptées.</li>
        <li>Saisissez uniquement ce code pour consulter le statut.</li>
      </ul>
    </AppCard>
  );
}
