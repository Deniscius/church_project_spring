import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { useUIStore } from '../../store/ui.context';
import AppIcon from '../ui/AppIcon';

export default function Topbar() {
  const { user, logout } = useAuth();
  const { activeParish, parishOptions, setActiveParish } = useTenant();
  const { setSidebarOpen } = useUIStore();

  return (
    <div className="admin-topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="mobile-menu-btn"
          onClick={() => setSidebarOpen(true)}
          aria-label="Ouvrir le menu"
        >
          <AppIcon name="menu" />
        </button>
        <div>
          <strong>{activeParish?.name || '—'}</strong>
        {parishOptions.length > 1 ? (
          <div className="topbar-parish-picker">
            <label htmlFor="topbar-parish" className="muted">
              Paroisse active
            </label>
            <select
              id="topbar-parish"
              className="select"
              value={activeParish?.id || ''}
              onChange={(e) => {
                const next = parishOptions.find((p) => p.id === e.target.value);
                if (next) setActiveParish(next);
              }}
            >
              {parishOptions.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
        ) : (
          <div className="topbar-context">Tableau privé de la paroisse connectée</div>
        )}
        </div>
      </div>
      <div className="button-row">
        <span className="badge">{user?.username || 'Invité'}</span>
        <button className="btn btn-secondary" type="button" onClick={logout}>
          <AppIcon name="logout" size={17} />
          Déconnexion
        </button>
      </div>
    </div>
  );
}
