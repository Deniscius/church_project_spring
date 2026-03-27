import React from 'react';
import AppCard from '../ui/AppCard';

export default function RequestSummaryCard() {
  return (
    <AppCard title="RequestSummaryCard" subtitle="Composant de présentation prêt à brancher sur les données backend.">
      <p style={{ margin: 0, color: 'var(--muted)' }}>Zone réservée à requestsummary.</p>
    </AppCard>
  );
}
