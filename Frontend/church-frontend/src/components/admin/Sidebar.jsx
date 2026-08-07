import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { useTenant } from '../../hooks/useTenant';
import { PERMISSIONS } from '../../constants/roles';
import { formatRole } from '../../utils/roleMapper';
import { useUIStore } from '../../store/ui.context';
import AppIcon from '../ui/AppIcon';

const parishMenu = [
  { to: '/admin/dashboard', label: 'Tableau de bord', icon: 'dashboard', permission: PERMISSIONS.DASHBOARD_VIEW },
  { to: '/admin/demandes', label: 'Demandes', icon: 'requests', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/feuille-intentions', label: "Feuille d'intentions", icon: 'celebrations', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/paiements', label: 'Paiements', icon: 'payments', permission: PERMISSIONS.PAYMENT_READ },
  { to: '/admin/tresorerie', label: 'Trésorerie', icon: 'invoices', permission: PERMISSIONS.TREASURY_READ },
  { to: '/admin/factures', label: 'Factures', icon: 'invoices', permission: PERMISSIONS.INVOICE_READ },
  { to: '/admin/horaires', label: 'Horaires', icon: 'schedules', permission: PERMISSIONS.SCHEDULE_READ },
  { to: '/admin/types-demandes', label: 'Types de demande', icon: 'types', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/forfaits', label: 'Forfaits', icon: 'pricing', permission: PERMISSIONS.PRICING_READ },
  { to: '/admin/recu', label: 'Reçu PDF', icon: 'invoices', permission: PERMISSIONS.RECEIPT_MANAGE },
  { to: '/admin/equipe', label: 'Équipe', icon: 'team', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/profil', label: 'Profil', icon: 'profile', permission: PERMISSIONS.PROFILE_READ },
];

const superMenu = [
  { to: '/admin/paroisses', label: 'Paroisses', icon: 'parishes', permission: PERMISSIONS.PARISH_MANAGE },
  { to: '/admin/inscriptions-paroisse', label: 'Inscriptions', icon: 'parishes', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/abonnements', label: 'Abonnements', icon: 'invoices', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/reversements', label: 'Reversements', icon: 'payments', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/catalogue-modele', label: 'Catalogue plateforme', icon: 'pricing', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/utilisateurs', label: 'Utilisateurs', icon: 'users', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/acces-paroisses', label: 'Accès paroisses', icon: 'access', permission: PERMISSIONS.PARISH_ACCESS_MANAGE },
  { to: '/admin/doyennes', label: 'Doyennés', icon: 'deaneries', permission: PERMISSIONS.DEANERY_MANAGE },
  { to: '/admin/types-paiement', label: 'Types de paiement', icon: 'paymentTypes', permission: PERMISSIONS.PAYMENT_TYPE_MANAGE },
  { to: '/admin/profil', label: 'Profil', icon: 'profile', permission: PERMISSIONS.PROFILE_READ },
];

export default function Sidebar() {
  const { user } = useAuth();
  const { has } = usePermissions();
  const { activeParish } = useTenant();
  const { sidebarOpen, setSidebarOpen } = useUIStore();
  const showPlatform = user?.isGlobal === true
    && (user?.role === 'SUPER_ADMIN' || user?.role === 'COMPTABLE');
  const menu = (showPlatform ? superMenu : parishMenu).filter((item) => has(item.permission));

  return (
    <>
      <button
        type="button"
        className={`sidebar-overlay${sidebarOpen ? ' open' : ''}`}
        onClick={() => setSidebarOpen(false)}
        aria-label="Fermer le menu"
      />
      <aside className={`sidebar${sidebarOpen ? ' open' : ''}`} aria-label="Navigation principale">
        <div className="sidebar-brand">
          <div className="sidebar-brand-row">
            <div>
              <div className="sidebar-brand-title">Missanye</div>
              <p className="muted">{activeParish?.name || 'Administration'}</p>
              <p className="muted">{user ? formatRole(user.role) : 'Aucun rôle'}</p>
            </div>
            <button
              type="button"
              className="sidebar-close"
              onClick={() => setSidebarOpen(false)}
              aria-label="Fermer le menu"
            >
              <AppIcon name="close" />
            </button>
          </div>
        </div>
        <nav className="sidebar-nav">
          {menu.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
            >
              <AppIcon name={item.icon} size={19} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}
