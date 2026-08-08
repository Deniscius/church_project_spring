import React from 'react';
import { Link } from 'react-router-dom';
import { PUBLIC_INDEXABLE_ROUTES } from '../seo/siteMeta';

/**
 * 404 soft : page dédiée noindex (évite de masquer les vraies erreurs
 * en redirigeant silencieusement vers l’accueil).
 */
export default function NotFoundPage() {
  return (
    <div className="container stack not-found-page" style={{ maxWidth: 640, paddingTop: 48, paddingBottom: 64 }}>
      <span className="badge warning" style={{ alignSelf: 'flex-start' }}>404 · Page introuvable</span>
      <h1 className="page-title">Cette page n’existe pas</h1>
      <p className="page-subtitle">
        Le lien est peut-être obsolète, ou l’adresse comporte une faute de frappe.
        Voici les pages publiques les plus utiles :
      </p>
      <ul className="site-map-list">
        {PUBLIC_INDEXABLE_ROUTES.filter((r) => r.path !== '/plan-du-site').map((route) => (
          <li key={route.path}>
            <Link to={route.path}>{route.label}</Link>
            <p className="muted">{route.description}</p>
          </li>
        ))}
      </ul>
      <div className="button-row">
        <Link className="btn btn-primary" to="/">
          Retour à l’accueil
        </Link>
        <Link className="btn btn-secondary" to="/plan-du-site">
          Plan du site
        </Link>
      </div>
    </div>
  );
}
