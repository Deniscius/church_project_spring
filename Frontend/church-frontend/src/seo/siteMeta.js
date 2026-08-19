/** Identité publique + métadonnées SEO / agentiques (SPA). */

export const SITE_NAME = 'Missanye';
export const SITE_TAGLINE = 'Intentions & célébrations';
export const SITE_DEFAULT_DESCRIPTION =
  'Déposez une intention de messe, suivez votre demande et consultez les horaires des paroisses actives.';

/** Organisation (JSON-LD, llms.txt, footer). */
export const SITE_ORG = {
  legalName: 'Missanye',
  shortName: SITE_NAME,
  areaServed: 'Lomé, Togo',
  inLanguage: 'fr',
  email: 'noreply@missanye.com',
};

/**
 * URL canonique du front (sitemap, OG absolus, JSON-LD).
 * En local, DocumentMeta retombe sur window.location.origin.
 */
export const SITE_ORIGIN = (
  import.meta.env.VITE_PUBLIC_SITE_URL || 'https://www.missanye.com'
).replace(/\/+$/, '');

export const SITE_IMAGE_PATH = '/assets/og-cover.jpg';
/** Visuel hero LCP (SVG léger) — distinct de l’image OG raster. */
export const SITE_HERO_PATH = '/assets/hero-sanctuary.svg';
export const SITE_OG_IMAGE_WIDTH = '1200';
export const SITE_OG_IMAGE_HEIGHT = '675';
export const SITE_OG_IMAGE_ALT = 'Missanye — Intentions et célébrations';

/** Pages indexables — source unique pour sitemap, plan du site, llms.txt. */
export const PUBLIC_INDEXABLE_ROUTES = [
  {
    path: '/',
    label: 'Accueil',
    description: 'Présentation de la plateforme et accès aux démarches fidèles.',
    changefreq: 'daily',
    priority: '1.0',
  },
  {
    path: '/demande',
    label: 'Faire une demande de messe',
    description: 'Déposer une intention : paroisse, dates, forfait et paiement.',
    changefreq: 'weekly',
    priority: '0.9',
  },
  {
    path: '/suivi',
    label: 'Suivre une demande',
    description: 'Consulter le statut via le code de suivi reçu lors du dépôt.',
    changefreq: 'monthly',
    priority: '0.8',
  },
  {
    path: '/horaires',
    label: 'Horaires des messes',
    description: 'Programmation des messes des paroisses actives, par doyenné.',
    changefreq: 'daily',
    priority: '0.85',
  },
  {
    path: '/offres',
    label: 'Offres pour les paroisses',
    description: 'Formules d’abonnement Missanye pour les paroisses, prix et durées à jour.',
    changefreq: 'weekly',
    priority: '0.8',
  },
  {
    path: '/inscription-paroisse',
    label: 'Inscrire ma paroisse',
    description: 'Demande d’inscription d’une paroisse à la plateforme SaaS.',
    changefreq: 'monthly',
    priority: '0.7',
  },
  {
    path: '/plan-du-site',
    label: 'Plan du site',
    description: 'Liste des pages publiques pour la navigation humaine et agentique.',
    changefreq: 'monthly',
    priority: '0.4',
  },
];

export const ROUTE_META = [
  {
    test: (p) => p === '/',
    title: `${SITE_NAME} — Demandes de messes & horaires`,
    description:
      'Plateforme paroissiale pour déposer une intention de messe, suivre son avancement et consulter les horaires des célébrations.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/demande/recapitulatif' || p === '/demande/confirmation',
    title: `Demande de messe — ${SITE_NAME}`,
    description: 'Étape privée du parcours de dépôt d’intention de messe.',
    robots: 'noindex, nofollow',
    ogType: 'website',
  },
  {
    test: (p) => p === '/demande' || p.startsWith('/demande/'),
    title: `Faire une demande de messe — ${SITE_NAME}`,
    description:
      'Choisissez votre paroisse, la nature de la célébration, les dates et le mode de paiement pour déposer une intention.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/suivi/resultat',
    title: `Résultat de suivi — ${SITE_NAME}`,
    description: 'Détail d’une demande de messe identifiée par son code de suivi.',
    robots: 'noindex, nofollow',
    ogType: 'website',
  },
  {
    test: (p) => p === '/suivi' || p.startsWith('/suivi/'),
    title: `Suivre une demande — ${SITE_NAME}`,
    description: 'Consultez le statut de validation et de paiement de votre demande grâce au code de suivi.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/horaires',
    title: `Horaires des messes — ${SITE_NAME}`,
    description:
      'Consultez la programmation des messes par doyenné et paroisse (abonnements actifs uniquement).',
    ogType: 'website',
  },
  {
    test: (p) => p === '/offres',
    title: `Offres paroisses — ${SITE_NAME}`,
    description:
      'Comparez les formules d’abonnement Missanye pour votre paroisse : prix en FCFA, durée, coût mensuel équivalent et inscription guidée.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/inscription-paroisse',
    title: `Inscrire ma paroisse — ${SITE_NAME}`,
    description:
      'Inscrivez votre paroisse pour gérer les demandes de messes, les horaires, la trésorerie et l’équipe.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/plan-du-site',
    title: `Plan du site — ${SITE_NAME}`,
    description:
      'Navigation structurée des pages publiques : demandes, suivi, horaires, offres et inscription paroisse.',
    ogType: 'website',
  },
  {
    test: (p) => p === '/admin/login',
    title: `Connexion espace paroisse — ${SITE_NAME}`,
    description: 'Accédez à l’espace sécurisé de votre paroisse pour gérer intentions, célébrations et trésorerie.',
    robots: 'noindex, follow',
    ogType: 'website',
  },
  {
    test: (p) => p.startsWith('/admin') || p.startsWith('/superadmin'),
    title: `Administration — ${SITE_NAME}`,
    description: 'Espace d’administration paroissiale.',
    robots: 'noindex, nofollow',
    ogType: 'website',
  },
  {
    test: (p) => p.startsWith('/facture/') || p.startsWith('/paiement/'),
    title: `Document — ${SITE_NAME}`,
    description: 'Consultation d’une facture ou d’un paiement lié à une demande de messe.',
    robots: 'noindex, nofollow',
    ogType: 'website',
  },
  {
    test: () => true,
    title: `Page introuvable — ${SITE_NAME}`,
    description: 'La page demandée n’existe pas ou n’est plus disponible.',
    robots: 'noindex, follow',
    ogType: 'website',
  },
];

export function resolveRouteMeta(pathname) {
  const match = ROUTE_META.find((entry) => entry.test(pathname));
  return {
    title: match?.title || SITE_NAME,
    description: match?.description || SITE_DEFAULT_DESCRIPTION,
    robots: match?.robots || 'index, follow',
    ogType: match?.ogType || 'website',
  };
}

/** Fil d’Ariane logique pour JSON-LD (pages publiques indexables). */
export function resolveBreadcrumbs(pathname) {
  if (pathname === '/') {
    return [{ name: 'Accueil', path: '/' }];
  }
  const crumbs = [{ name: 'Accueil', path: '/' }];
  const hit = PUBLIC_INDEXABLE_ROUTES.find((r) => r.path === pathname);
  if (hit && hit.path !== '/') {
    crumbs.push({ name: hit.label, path: hit.path });
    return crumbs;
  }
  if (pathname.startsWith('/demande')) {
    crumbs.push({ name: 'Faire une demande', path: '/demande' });
    return crumbs;
  }
  if (pathname.startsWith('/suivi')) {
    crumbs.push({ name: 'Suivre une demande', path: '/suivi' });
    return crumbs;
  }
  return crumbs;
}

export function absoluteUrl(path, origin = SITE_ORIGIN) {
  const base = (origin || SITE_ORIGIN).replace(/\/+$/, '');
  if (!path || path === '/') return `${base}/`;
  return `${base}${path.startsWith('/') ? path : `/${path}`}`;
}
