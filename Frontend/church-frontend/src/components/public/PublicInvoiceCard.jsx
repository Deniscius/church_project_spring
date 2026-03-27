import React from 'react';
import AppCard from '../ui/AppCard';

export default function PublicInvoiceCard() {
  return (
    <AppCard title="PublicInvoiceCard" subtitle="Composant de présentation prêt à brancher sur les données backend.">
      <p style={{ margin: 0, color: 'var(--muted)' }}>Zone réservée à publicinvoice.</p>
    </AppCard>
  );
}
