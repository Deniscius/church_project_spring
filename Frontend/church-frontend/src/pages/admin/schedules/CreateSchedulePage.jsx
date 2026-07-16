import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import ScheduleForm from './ScheduleForm';

export default function CreateSchedulePage() {
  return (
    <div className="stack">
      <PageHeader title="Créer un horaire" subtitle="Formulaire de création d'un horaire rattaché à la paroisse active." />
      <ScheduleForm />
    </div>
  );
}
