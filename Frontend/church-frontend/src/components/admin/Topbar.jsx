import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';

export default function Topbar() {
  const { user, logout } = useAuth();
  const { activeParish } = useTenant();

  return (
    <div className="admin-topbar">
      <div>
        <strong>{activeParish?.name}</strong>
        <div style={{ color: 'var(--muted)', marginTop: 4 }}>Tableau privé de la paroisse connectée</div>
      </div>
      <div className="button-row">
        <span className="badge">{user?.username || 'Invité'}</span>
        <button className="btn btn-secondary" type="button" onClick={logout}>
          Déconnexion
        </button>
      </div>
    </div>
  );
}
