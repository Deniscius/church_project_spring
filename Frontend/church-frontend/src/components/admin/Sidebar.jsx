import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { useTenant } from '../../hooks/useTenant';
import { PERMISSIONS } from '../../constants/roles';
import { formatRole } from '../../utils/roleMapper';

const parishMenu = [
  { to: '/admin/dashboard', label: 'Dashboard', permission: PERMISSIONS.DASHBOARD_VIEW },
  { to: '/admin/demandes', label: 'Demandes', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/paiements', label: 'Paiements', permission: PERMISSIONS.PAYMENT_READ },
  { to: '/admin/factures', label: 'Factures', permission: PERMISSIONS.INVOICE_READ },
  { to: '/admin/horaires', label: 'Horaires', permission: PERMISSIONS.SCHEDULE_READ },
  { to: '/admin/types-demandes', label: 'Types de demande', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/forfaits', label: 'Forfaits', permission: PERMISSIONS.PRICING_READ },
  { to: '/admin/equipe', label: 'Équipe', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/profil', label: 'Profil', permission: PERMISSIONS.PROFILE_READ },
];

const superMenu = [
  { to: '/admin/paroisses', label: 'Paroisses', permission: PERMISSIONS.PARISH_MANAGE },
  { to: '/admin/utilisateurs', label: 'Utilisateurs', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/acces-paroisses', label: 'Accès paroisses', permission: PERMISSIONS.PARISH_ACCESS_MANAGE },
  { to: '/admin/doyennes', label: 'Doyennés', permission: PERMISSIONS.DEANERY_MANAGE },
  { to: '/admin/types-paiement', label: 'Types de paiement', permission: PERMISSIONS.PAYMENT_TYPE_MANAGE },
];

export default function Sidebar() {
  const { user } = useAuth();
  const { has } = usePermissions();
  const { activeParish } = useTenant();
  const showSuper = user?.role === 'SUPER_ADMIN' && user?.isGlobal === true;
  const menu = (showSuper ? superMenu : parishMenu).filter((item) => has(item.permission));

  return (
    <aside className="sidebar">
      <div>
        <div style={{ fontWeight: 800, fontSize: 20 }}>Church Admin</div>
        <p className="muted">{activeParish?.name}</p>
        <p className="muted">{user ? formatRole(user.role) : 'Aucun rôle'}</p>
      </div>
      <nav className="sidebar-nav">
        {menu.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
