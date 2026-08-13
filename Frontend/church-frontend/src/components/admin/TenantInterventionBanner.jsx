import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { tenantStatusLabel } from '../../utils/statusMapper';
import { ROUTES } from '../../constants/routes';

/**
 * Bandeau visible en mode intervention plateforme (Super Admin / Comptable).
 * Rappelle que l’on n’est pas l’équipe de la paroisse, mais un contrôle / audit.
 */
export default function TenantInterventionBanner() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { activeParish, setActiveParish, isIntervening } = useTenant();

  const isPlatformStaff = user?.isGlobal === true
    && (user?.role === 'SUPER_ADMIN' || user?.role === 'COMPTABLE');

  if (!isIntervening || !isPlatformStaff) {
    return null;
  }

  const isComptable = user?.role === 'COMPTABLE';
  const exitTo = isComptable ? ROUTES.PLATFORM_DEMANDES : ROUTES.PARISHES;
  const directoryLabel = isComptable ? 'Audit demandes' : 'Annuaire';

  const exit = () => {
    setActiveParish(null);
    navigate(exitTo);
  };

  return (
    <aside className="tenant-intervention-banner" role="status">
      <div className="tenant-intervention-banner-main">
        <span className="tenant-intervention-badge">Intervention</span>
        <div>
          <strong>{activeParish?.name}</strong>
          <p>
            {isComptable
              ? 'Audit / traçabilité plateforme'
              : 'Contrôle administratif plateforme'}
            {activeParish?.city ? ` · ${activeParish.city}` : ''}
            {activeParish?.statutTenant
              ? ` · ${tenantStatusLabel(activeParish.statutTenant)}`
              : ''}
            . Les actions impactent ce tenant — quittez dès que le souci est traité.
          </p>
        </div>
      </div>
      <div className="tenant-intervention-banner-actions">
        <Link className="btn btn-secondary btn-sm" to={exitTo}>
          {directoryLabel}
        </Link>
        <button type="button" className="btn btn-secondary btn-sm" onClick={exit}>
          Quitter l’intervention
        </button>
      </div>
    </aside>
  );
}
