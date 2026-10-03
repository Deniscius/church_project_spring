import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import PastoralYearForm from './PastoralYearForm';

export default function CreatePastoralYearPage() {
  return (
    <div className="stack">
      <PageHeader
        title="Créer une année pastorale"
        subtitle="Définissez la période qui structurera l’agenda et les activités de la paroisse."
      />
      <PastoralYearForm />
    </div>
  );
}
