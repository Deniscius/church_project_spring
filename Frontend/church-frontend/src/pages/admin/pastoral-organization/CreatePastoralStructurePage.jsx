import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import StructurePastoraleForm from './StructurePastoraleForm';

export default function CreatePastoralStructurePage() {
  return (
    <div className="stack">
      <PageHeader
        title="Créer une structure pastorale"
        subtitle="Ajoutez une instance, une commission, un groupe, une chorale ou un mouvement."
      />
      <StructurePastoraleForm />
    </div>
  );
}
