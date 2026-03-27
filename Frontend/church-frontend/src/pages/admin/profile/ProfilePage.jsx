import React from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import { useAuth } from '../../../hooks/useAuth';
import { useTenant } from '../../../hooks/useTenant';
import { formatRole } from '../../../utils/roleMapper';

export default function ProfilePage() {
  const { user } = useAuth();
  const { activeParish } = useTenant();

  return (
    <div className="stack">
      <PageHeader title="Profil" subtitle="Résumé de l'utilisateur connecté et de son contexte paroisse." />
      <AppCard title="Compte connecté">
        <div className="info-list">
          <div className="info-row"><span>Nom</span><strong>{user?.firstName} {user?.lastName}</strong></div>
          <div className="info-row"><span>Username</span><span>{user?.username}</span></div>
          <div className="info-row"><span>Rôle</span><span>{formatRole(user?.role)}</span></div>
          <div className="info-row"><span>Paroisse active</span><span>{activeParish?.name}</span></div>
        </div>
      </AppCard>
    </div>
  );
}
