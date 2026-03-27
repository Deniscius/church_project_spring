import React from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppCard from '../../components/ui/AppCard';
import AppButton from '../../components/ui/AppButton';

export default function HomePage() {
  return (
    <div className="stack">
      <section className="hero">
        <PageHeader
          title="Système de gestion des demandes de messes"
          subtitle="Point d'entrée public pour déposer une demande, suivre son avancement et consulter les informations de paiement ou de facture."
          actions={
            <div className="button-row">
              <Link to="/demande"><AppButton>Faire une demande</AppButton></Link>
              <Link to="/suivi"><AppButton variant="secondary">Suivre une demande</AppButton></Link>
            </div>
          }
        />
      </section>

      <div className="card-grid">
        <AppCard title="Faire une demande" subtitle="Parcours public sans authentification.">
          <p>Le fidèle choisit sa paroisse, son type de demande, ses dates, son horaire et son moyen de paiement.</p>
        </AppCard>
        <AppCard title="Suivre une demande" subtitle="Recherche par code de suivi.">
          <p>Consultation simple du statut de validation, du statut de paiement et des liens utiles.</p>
        </AppCard>
        <AppCard title="Espace paroisse" subtitle="Dashboard privé par paroisse.">
          <p>Chaque paroisse accède à ses propres données, jamais à celles des autres.</p>
        </AppCard>
      </div>
    </div>
  );
}
