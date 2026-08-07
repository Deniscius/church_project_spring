import React from 'react';
import { useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import TeamMemberForm from './TeamMemberForm';

export default function EditTeamMemberPage() {
  const { id } = useParams();

  return (
    <div className="stack">
      <PageHeader
        title="Modifier un membre"
        subtitle="Mise à jour du compte et de son rôle applicatif."
      />
      <TeamMemberForm userId={id} />
    </div>
  );
}
