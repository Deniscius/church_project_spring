import React from 'react';
import AppCard from '../ui/AppCard';

export default function PublicPaymentCard() {
  return (
    <AppCard title="PublicPaymentCard" subtitle="Composant de présentation prêt à brancher sur les données backend.">
      <p style={{ margin: 0, color: 'var(--muted)' }}>Zone réservée à publicpayment.</p>
    </AppCard>
  );
}
