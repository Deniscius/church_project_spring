import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import MandatPastoralForm from './MandatPastoralForm';

export default function CreatePastoralMandatePage() {
  return (
    <div className="stack">
      <PageHeader
        title="Attribuer une responsabilité"
        subtitle="Associez une personne, une fonction et une structure pour l’année pastorale."
      />
      <MandatPastoralForm />
    </div>
  );
}
