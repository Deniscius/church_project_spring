import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import ScheduleForm from './ScheduleForm';

export default function EditSchedulePage() {
  const { id } = useParams();
  return (
    <div className="stack">
      <PageHeader title="Modifier un horaire" subtitle="Version d'édition du formulaire horaire." />
      <ScheduleForm scheduleId={id} />
    </div>
  );
}
