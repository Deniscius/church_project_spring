import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { useUIStore } from '../../store/ui.context';
import AppIcon from '../ui/AppIcon';
import ThemeToggle from '../ui/ThemeToggle';
import { tenantStatusLabel } from '../../utils/statusMapper';

export default function Topbar() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { activeParish, parishOptions, setActiveParish, isIntervening } = useTenant();
  const { setSidebarOpen } = useUIStore();
  const isSuperAdmin = user?.isGlobal === true && user?.role === 'SUPER_ADMIN';
  const [tenantQuery, setTenantQuery] = useState('');

  const filteredOptions = useMemo(() => {
    const q = tenantQuery.trim().toLowerCase();
    let list = parishOptions;
    if (q && isSuperAdmin) {
      list = parishOptions.filter((p) => {
        const hay = [p.name, p.city, p.statutTenant]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return hay.includes(q);
      });
    }
    if (
      activeParish?.id
      && !activeParish?.isSystem
      && !list.some((p) => p.id === activeParish.id)
    ) {
      list = [activeParish, ...list];
    }
    return list;
  }, [parishOptions, tenantQuery, isSuperAdmin, activeParish]);

  const showPicker = isSuperAdmin
    ? parishOptions.length > 0
    : parishOptions.length > 1;

  const onPick = (value) => {
    if (!value) {
      setActiveParish(null);
      if (isSuperAdmin) navigate('/admin/paroisses');
      return;
    }
    const next = parishOptions.find((p) => p.id === value);
    if (!next) return;
    setActiveParish(next);
    setTenantQuery('');
    if (isSuperAdmin) navigate('/admin/dashboard');
  };

  const title = isIntervening
    ? activeParish?.name
    : (isSuperAdmin ? 'Plateforme' : (activeParish?.name || 'Administration'));

  return (
    <header className="admin-topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="mobile-menu-btn"
          onClick={() => setSidebarOpen(true)}
          aria-label="Ouvrir le menu"
        >
          <AppIcon name="menu" size={18} />
        </button>

        <div className="topbar-title-block">
          <strong className="topbar-title" title={title || undefined}>{title}</strong>
          {isIntervening ? (
            <span className="topbar-pill">Intervention</span>
          ) : null}
        </div>

        {showPicker ? (
          <div className="topbar-parish-picker">
            {isSuperAdmin ? (
              <input
                type="search"
                className="input topbar-tenant-search"
                placeholder="Rechercher…"
                value={tenantQuery}
                onChange={(e) => setTenantQuery(e.target.value)}
                aria-label="Rechercher un tenant"
              />
            ) : null}
            <select
              id="topbar-parish"
              className="select topbar-parish-select"
              value={activeParish?.isSystem ? '' : (activeParish?.id || '')}
              onChange={(e) => onPick(e.target.value)}
              aria-label={isSuperAdmin ? 'Intervenir sur un tenant' : 'Paroisse active'}
            >
              {isSuperAdmin ? (
                <option value="">Plateforme</option>
              ) : null}
              {filteredOptions.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                  {p.city ? ` · ${p.city}` : ''}
                  {p.statutTenant ? ` (${tenantStatusLabel(p.statutTenant)})` : ''}
                </option>
              ))}
            </select>
          </div>
        ) : null}
      </div>

      <div className="topbar-actions">
        {isIntervening ? (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setActiveParish(null);
              navigate('/admin/paroisses');
            }}
          >
            Quitter
          </button>
        ) : null}
        <ThemeToggle compact className="topbar-theme-toggle" />
        <span className="topbar-user" title={user?.username || ''}>
          {user?.username || 'Invité'}
        </span>
        <button
          className="btn btn-ghost topbar-logout"
          type="button"
          onClick={logout}
          aria-label="Déconnexion"
          title="Déconnexion"
        >
          <AppIcon name="logout" size={16} />
          <span className="topbar-logout-label">Déconnexion</span>
        </button>
      </div>
    </header>
  );
}
