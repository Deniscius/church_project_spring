import React from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import { PUBLIC_INDEXABLE_ROUTES } from '../../seo/siteMeta';

const AGENT_LINKS = [
  { href: '/llms.txt', label: 'llms.txt', hint: 'Guide pour assistants IA' },
  { href: '/sitemap.xml', label: 'sitemap.xml', hint: 'Index des URLs publiques' },
  { href: '/robots.txt', label: 'robots.txt', hint: 'Règles d’exploration' },
];

export default function SiteMapPage() {
  return (
    <div className="container stack site-map-page">
      <PageHeader
        title="Plan du site"
        subtitle="Pages publiques utiles aux fidèles, aux paroisses et aux outils de découverte (SEO, agents)."
      />

      <section aria-labelledby="pages-publiques-title" className="site-map-section">
        <h2 id="pages-publiques-title" className="site-map-heading">
          Pages publiques
        </h2>
        <ul className="site-map-list">
          {PUBLIC_INDEXABLE_ROUTES.map((route) => (
            <li key={route.path}>
              <Link to={route.path}>{route.label}</Link>
              <p className="muted">{route.description}</p>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="espace-paroisse-title" className="site-map-section">
        <h2 id="espace-paroisse-title" className="site-map-heading">
          Espace paroisse
        </h2>
        <ul className="site-map-list">
          <li>
            <Link to="/admin/login">Connexion</Link>
            <p className="muted">Accès réservé aux équipes paroissiales (non indexé).</p>
          </li>
        </ul>
      </section>

      <section aria-labelledby="decouverte-title" className="site-map-section">
        <h2 id="decouverte-title" className="site-map-heading">
          Découverte technique
        </h2>
        <ul className="site-map-list">
          {AGENT_LINKS.map((item) => (
            <li key={item.href}>
              <a href={item.href}>{item.label}</a>
              <p className="muted">{item.hint}</p>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
