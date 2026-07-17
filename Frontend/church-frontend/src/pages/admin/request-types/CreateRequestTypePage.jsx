import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import RequestTypeForm from './RequestTypeForm';

export default function CreateRequestTypePage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un type de demande" subtitle="Formulaire de création pour les catégories liturgiques et assimilées." />
      <RequestTypeForm />
    </div>
  );
}
