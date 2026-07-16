import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import RequestTypeForm from './RequestTypeForm';

export default function EditRequestTypePage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader title="Modifier un type de demande" subtitle="Version d'édition du référentiel type de demande." />
      <RequestTypeForm requestTypeId={id} />
    </div>
  );
}
