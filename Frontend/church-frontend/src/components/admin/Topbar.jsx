import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';

export default function Topbar() {
  const { user, logout } = useAuth();
  const { activeParish, parishOptions, setActiveParish } = useTenant();

  return (
    <div className="admin-topbar">
      <div>
        <strong>{activeParish?.name || '—'}</strong>
        {parishOptions.length > 1 ? (
          <div className="form-field" style={{ marginTop: 8, maxWidth: 320 }}>
            <label htmlFor="topbar-parish" className="muted" style={{ fontSize: 12 }}>
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
          <div style={{ color: 'var(--muted)', marginTop: 4 }}>Tableau privé de la paroisse connectée</div>
        )}
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
