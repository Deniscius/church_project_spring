import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { useTenant } from '../../hooks/useTenant';
import { PERMISSIONS } from '../../constants/roles';
import { formatRole } from '../../utils/roleMapper';
import { useUIStore } from '../../store/ui.context';
import AppIcon from '../ui/AppIcon';
import BrandLogo from '../ui/BrandLogo';

/** Menu quotidien de l’équipe paroissiale. */
const parishMenu = [
  { to: '/admin/dashboard', label: 'Tableau de bord', icon: 'dashboard', permission: PERMISSIONS.DASHBOARD_VIEW },
  { to: '/admin/demandes', label: 'Demandes', icon: 'requests', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/feuille-intentions', label: 'Feuille d’intentions', icon: 'celebrations', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/paiements', label: 'Paiements', icon: 'payments', permission: PERMISSIONS.PAYMENT_READ },
  { to: '/admin/tresorerie', label: 'Trésorerie', icon: 'treasury', permission: PERMISSIONS.TREASURY_READ },
  { to: '/admin/factures', label: 'Factures', icon: 'invoices', permission: PERMISSIONS.INVOICE_READ },
  { to: '/admin/horaires', label: 'Horaires', icon: 'schedules', permission: PERMISSIONS.SCHEDULE_READ },
  { to: '/admin/types-demandes', label: 'Types de demandes', icon: 'types', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/forfaits', label: 'Forfaits', icon: 'pricing', permission: PERMISSIONS.PRICING_READ },
  { to: '/admin/recu', label: 'Reçu', icon: 'invoices', permission: PERMISSIONS.RECEIPT_MANAGE },
  { to: '/admin/equipe', label: 'Équipe', icon: 'team', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/profil', label: 'Mon profil', icon: 'profile', permission: PERMISSIONS.PROFILE_READ },
];

/** Contrôle Super Admin sur un tenant (intervention). */
const interventionMenu = [
  { to: '/admin/dashboard', label: 'Vue d’ensemble', icon: 'dashboard', permission: PERMISSIONS.DASHBOARD_VIEW },
  { to: '/admin/demandes', label: 'Demandes', icon: 'requests', permission: PERMISSIONS.DEMAND_READ },
  { to: '/admin/paiements', label: 'Paiements', icon: 'payments', permission: PERMISSIONS.PAYMENT_READ },
  { to: '/admin/tresorerie', label: 'Trésorerie', icon: 'treasury', permission: PERMISSIONS.TREASURY_READ },
  { to: '/admin/horaires', label: 'Horaires', icon: 'schedules', permission: PERMISSIONS.SCHEDULE_READ },
  { to: '/admin/types-demandes', label: 'Types de demandes', icon: 'types', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/forfaits', label: 'Forfaits', icon: 'pricing', permission: PERMISSIONS.PRICING_READ },
  { to: '/admin/equipe', label: 'Accès équipe', icon: 'team', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/recu', label: 'Reçu', icon: 'invoices', permission: PERMISSIONS.RECEIPT_MANAGE },
];

/** Menu plateforme (Super Admin / Comptable SaaS). */
const platformMenu = [
  { to: '/admin/paroisses', label: 'Paroisses', icon: 'parishes', permission: PERMISSIONS.PARISH_MANAGE },
  { to: '/admin/inscriptions-paroisse', label: 'Inscriptions', icon: 'parishes', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/abonnements', label: 'Abonnements', icon: 'invoices', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/reversements', label: 'Reversements', icon: 'payments', permission: PERMISSIONS.FINANCE_READ },
  { to: '/admin/catalogue-modele', label: 'Catalogue', icon: 'pricing', permission: PERMISSIONS.REQUEST_TYPE_READ },
  { to: '/admin/utilisateurs', label: 'Utilisateurs', icon: 'users', permission: PERMISSIONS.USER_MANAGE },
  { to: '/admin/acces-paroisses', label: 'Accès paroisses', icon: 'access', permission: PERMISSIONS.PARISH_ACCESS_MANAGE },
  { to: '/admin/doyennes', label: 'Doyennés', icon: 'deaneries', permission: PERMISSIONS.DEANERY_MANAGE },
  { to: '/admin/types-paiement', label: 'Types de paiement', icon: 'paymentTypes', permission: PERMISSIONS.PAYMENT_TYPE_MANAGE },
  { to: '/admin/profil', label: 'Mon profil', icon: 'profile', permission: PERMISSIONS.PROFILE_READ },
];

function NavItems({ items, onNavigate }) {
  return items.map((item) => (
    <NavLink
      key={item.to}
      to={item.to}
      end={item.to === '/admin/dashboard'}
      onClick={onNavigate}
      className={({ isActive }) => `sidebar-link${isActive ? ' is-active' : ''}`}
    >
      <AppIcon name={item.icon} size={17} />
      <span>{item.label}</span>
    </NavLink>
  ));
}

export default function Sidebar() {
  const { user } = useAuth();
  const { has } = usePermissions();
  const { activeParish, isIntervening } = useTenant();
  const { sidebarOpen, setSidebarOpen } = useUIStore();

  const isPlatformUser = user?.isGlobal === true
    && (user?.role === 'SUPER_ADMIN' || user?.role === 'COMPTABLE');
  const isSuperAdmin = user?.role === 'SUPER_ADMIN';

  const visiblePlatform = isPlatformUser
    ? platformMenu.filter((item) => has(item.permission))
    : [];

  let visibleParish = [];
  if (isSuperAdmin) {
    if (isIntervening) {
      visibleParish = interventionMenu.filter((item) => has(item.permission));
    }
  } else if (!isPlatformUser) {
    visibleParish = parishMenu.filter((item) => has(item.permission));
  }

  const close = () => setSidebarOpen(false);

  const parishLabel = isIntervening && activeParish?.name
    ? activeParish.name
    : (activeParish?.name || 'Paroisse');

  const modeLabel = isIntervening
    ? parishLabel
    : isSuperAdmin
      ? 'Espace plateforme'
      : parishLabel;

  return (
    <>
      <button
        type="button"
        className={`sidebar-overlay${sidebarOpen ? ' is-open' : ''}`}
        onClick={close}
        aria-label="Fermer le menu"
      />
      <aside
        className={`sidebar${sidebarOpen ? ' is-open' : ''}`}
        aria-label="Navigation principale"
      >
        <div className="sidebar-brand">
          <div className="sidebar-brand-main">
            <BrandLogo size={36} className="sidebar-brand-logo" alt="" />
            <div className="sidebar-brand-text">
              <div className="sidebar-brand-title">Missanye</div>
              <div className="sidebar-brand-meta">
                <span className="sidebar-brand-mode" title={modeLabel}>
                  {modeLabel}
                </span>
              </div>
              <div className="sidebar-brand-tags">
                {isIntervening ? (
                  <span className="sidebar-mode-badge sidebar-mode-badge--intervene">Intervention</span>
                ) : (
                  <span className="sidebar-brand-role">{user ? formatRole(user.role) : ''}</span>
                )}
              </div>
            </div>
          </div>
          <button
            type="button"
            className="sidebar-close"
            onClick={close}
            aria-label="Fermer le menu"
          >
            <AppIcon name="close" size={18} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {visiblePlatform.length > 0 && (
            <div className="sidebar-section">
              <p className="sidebar-section-label">Plateforme</p>
              <NavItems items={visiblePlatform} onNavigate={close} />
            </div>
          )}

          {visibleParish.length > 0 && (
            <div className="sidebar-section">
              <p className="sidebar-section-label">
                {isIntervening ? 'Contrôle paroisse' : 'Paroisse'}
              </p>
              <NavItems items={visibleParish} onNavigate={close} />
            </div>
          )}
        </nav>

        <div className="sidebar-footer">
          <span className="sidebar-footer-note">Missanye · Togo</span>
        </div>
      </aside>
    </>
  );
}
