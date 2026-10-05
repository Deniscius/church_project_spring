import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import PastoralActorForm from './PastoralActorForm';

export default function EditPastoralActorPage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader
        title="Modifier un acteur pastoral"
        subtitle="Mettez à jour l’identité et les coordonnées sans modifier son historique de fonctions."
      />
      <PastoralActorForm pastoralActorId={id} />
    </div>
  );
}
