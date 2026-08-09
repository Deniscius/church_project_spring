import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useTenant } from '../../hooks/useTenant';
import { tenantStatusLabel } from '../../utils/statusMapper';

/**
 * Bandeau visible uniquement en mode intervention Super Admin.
 * Rappelle que l’on n’est pas l’équipe de la paroisse, mais un contrôle plateforme.
 */
export default function TenantInterventionBanner() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { activeParish, setActiveParish, isIntervening } = useTenant();

  if (!isIntervening || user?.role !== 'SUPER_ADMIN') {
    return null;
  }

  const exit = () => {
    setActiveParish(null);
    navigate('/admin/paroisses');
  };

  return (
    <aside className="tenant-intervention-banner" role="status">
      <div className="tenant-intervention-banner-main">
        <span className="tenant-intervention-badge">Intervention</span>
        <div>
          <strong>{activeParish?.name}</strong>
          <p>
            Contrôle administratif plateforme
            {activeParish?.city ? ` · ${activeParish.city}` : ''}
            {activeParish?.statutTenant
              ? ` · ${tenantStatusLabel(activeParish.statutTenant)}`
              : ''}
            . Les actions impactent ce tenant — quittez dès que le souci est traité.
          </p>
        </div>
      </div>
      <div className="tenant-intervention-banner-actions">
        <Link className="btn btn-secondary btn-sm" to="/admin/paroisses">
          Annuaire
        </Link>
        <button type="button" className="btn btn-secondary btn-sm" onClick={exit}>
          Quitter l’intervention
        </button>
      </div>
    </aside>
  );
}
