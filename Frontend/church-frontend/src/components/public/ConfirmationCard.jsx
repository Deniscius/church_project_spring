import React from 'react';
import { Link } from 'react-router-dom';
import AppCard from '../ui/AppCard';

export default function ConfirmationCard({ result }) {
  if (!result?.codeSuivie) {
    return (
      <AppCard title="Aucune donnée" subtitle="Revenez au formulaire pour créer une demande.">
        <Link to="/demande" className="btn btn-primary" style={{ textDecoration: 'none' }}>
          Nouvelle demande
        </Link>
      </AppCard>
    );
  }

  return (
    <AppCard
      title="Demande enregistrée"
      subtitle="Conservez le code de suivi : il sert pour le suivi, la facture et le paiement."
    >
      <p style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700 }}>{result.codeSuivie}</p>
      <div className="button-row" style={{ marginTop: 16 }}>
        <Link
          to={`/suivi/resultat?code=${encodeURIComponent(result.codeSuivie)}`}
          className="btn btn-primary"
          style={{ textDecoration: 'none' }}
        >
          Suivre la demande
        </Link>
        <Link
          to={`/facture/${encodeURIComponent(result.codeSuivie)}`}
          className="btn btn-secondary"
          style={{ textDecoration: 'none' }}
        >
          Voir la facture
        </Link>
      </div>
    </AppCard>
  );
}
