import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import MandatPastoralForm from './MandatPastoralForm';

export default function EditPastoralMandatePage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader
        title="Modifier la responsabilité"
        subtitle="Mettez à jour la fonction, les attributions et la période du mandat."
      />
      <MandatPastoralForm mandateId={id} />
    </div>
  );
}
