import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import PastoralYearForm from './PastoralYearForm';

export default function EditPastoralYearPage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader
        title="Modifier l’année pastorale"
        subtitle="La modification est réservée aux années encore en brouillon."
      />
      <PastoralYearForm pastoralYearId={id} />
    </div>
  );
}
