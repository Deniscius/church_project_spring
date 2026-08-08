import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import TeamMemberForm from './TeamMemberForm';

export default function CreateTeamMemberPage() {
  return (
    <div className="stack">
      <PageHeader
        title="Nouveau membre"
        subtitle="Création d’un compte rattaché à la paroisse active."
      />
      <TeamMemberForm />
    </div>
  );
}
