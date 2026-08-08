import React from 'react';
import { useLocation } from 'react-router-dom';
import BackButton from './BackButton';

const parentRoutes = [
  [/^\/admin\/demandes\/[^/]+(?:\/modifier)?$/, '/admin/demandes'],
  [/^\/admin\/paiements\/[^/]+$/, '/admin/paiements'],
  [/^\/admin\/factures\/[^/]+$/, '/admin/factures'],
  [/^\/admin\/horaires\/(?:nouveau|[^/]+\/modifier)$/, '/admin/horaires'],
  [/^\/admin\/types-demandes\/(?:nouveau|[^/]+\/modifier)$/, '/admin/types-demandes'],
  [/^\/admin\/forfaits\/(?:nouveau|[^/]+\/modifier)$/, '/admin/forfaits'],
  [/^\/admin\/equipe\/(?:nouveau|[^/]+\/modifier)$/, '/admin/equipe'],
  [/^\/admin\/paroisses\/(?:nouvelle|[^/]+\/modifier)$/, '/admin/paroisses'],
  [/^\/admin\/utilisateurs\/(?:nouveau|[^/]+\/modifier)$/, '/admin/utilisateurs'],
  [/^\/suivi\/resultat$/, '/suivi'],
  [/^\/demande\/recapitulatif$/, '/demande'],
  [/^\/(?:facture|paiement)\/[^/]+$/, '/suivi'],
];

function inferBackRoute(pathname) {
  return parentRoutes.find(([pattern]) => pattern.test(pathname))?.[1] || null;
}

export default function PageHeader({
  title,
  subtitle,
  actions = null,
  backTo,
  backLabel = 'Retour',
}) {
  const { pathname } = useLocation();
  const resolvedBackTo = backTo === false ? null : (backTo || inferBackRoute(pathname));

  return (
    <div className="page-header">
      <div className="page-header-content">
        {resolvedBackTo ? <BackButton to={resolvedBackTo} label={backLabel} /> : null}
        <div>
          <h1 className="page-title">{title}</h1>
          {subtitle ? <p className="page-subtitle">{subtitle}</p> : null}
        </div>
      </div>
      {actions ? <div className="page-header-actions">{actions}</div> : null}
    </div>
  );
}
