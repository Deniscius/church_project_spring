import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import PastoralActorForm from './PastoralActorForm';

export default function CreatePastoralActorPage() {
  return (
    <div className="stack">
      <PageHeader
        title="Ajouter un acteur pastoral"
        subtitle="Créez une fiche personne réutilisable dans les mandats et les affectations."
      />
      <PastoralActorForm />
    </div>
  );
}
