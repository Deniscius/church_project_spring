import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import StructurePastoraleForm from './StructurePastoraleForm';

export default function EditPastoralStructurePage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader
        title="Modifier la structure pastorale"
        subtitle="Mettez à jour sa dénomination, sa mission et son ordre d’affichage."
      />
      <StructurePastoraleForm structureId={id} />
    </div>
  );
}
