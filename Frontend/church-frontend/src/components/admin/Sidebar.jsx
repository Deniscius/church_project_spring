import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { formatRole } from '../../utils/roleMapper';

const menu = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/demandes', label: 'Demandes' },
  { to: '/admin/paiements', label: 'Paiements' },
  { to: '/admin/factures', label: 'Factures' },
  { to: '/admin/horaires', label: 'Horaires' },
  { to: '/admin/types-demandes', label: 'Types de demande' },
  { to: '/admin/forfaits', label: 'Forfaits' },
  { to: '/admin/profil', label: 'Profil' },
  { to: '/admin/paroisses', label: 'Paroisses' },
  { to: '/admin/utilisateurs', label: 'Utilisateurs' },
  { to: '/admin/acces-paroisses', label: 'Accès paroisses' },
  { to: '/admin/localites', label: 'Localités' },
  { to: '/admin/types-paiement', label: 'Types de paiement' },
];

export default function Sidebar() {
  const { user } = useAuth();
  const { activeParish } = useTenant();

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
