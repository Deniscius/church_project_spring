import {
  SITE_DEFAULT_DESCRIPTION,
  SITE_NAME,
  SITE_ORG,
  absoluteUrl,
  resolveBreadcrumbs,
} from './siteMeta';

/**
 * Graphe JSON-LD (Organization + WebSite + WebPage + fil d’Ariane).
 * Une seule balise script par navigation.
 */
export function buildPageJsonLd({ pathname, title, description, origin, image }) {
  const pageUrl = absoluteUrl(pathname, origin);
  const homeUrl = absoluteUrl('/', origin);
  const crumbs = resolveBreadcrumbs(pathname);

  const organization = {
    '@type': 'Organization',
    '@id': `${homeUrl}#organization`,
    name: SITE_ORG.legalName,
    alternateName: SITE_ORG.shortName,
    url: homeUrl,
    email: SITE_ORG.email,
    areaServed: SITE_ORG.areaServed,
    logo: image,
    description: SITE_DEFAULT_DESCRIPTION,
  };

  const website = {
    '@type': 'WebSite',
    '@id': `${homeUrl}#website`,
    name: SITE_NAME,
    url: homeUrl,
    description: SITE_DEFAULT_DESCRIPTION,
    inLanguage: SITE_ORG.inLanguage,
    publisher: { '@id': `${homeUrl}#organization` },
  };

  const webPage = {
    '@type': 'WebPage',
    '@id': `${pageUrl}#webpage`,
    url: pageUrl,
    name: title,
    description: description || SITE_DEFAULT_DESCRIPTION,
    isPartOf: { '@id': `${homeUrl}#website` },
    about: { '@id': `${homeUrl}#organization` },
    inLanguage: SITE_ORG.inLanguage,
  };

  const graph = [organization, website, webPage];

  if (crumbs.length > 1) {
    graph.push({
      '@type': 'BreadcrumbList',
      '@id': `${pageUrl}#breadcrumb`,
      itemListElement: crumbs.map((item, index) => ({
        '@type': 'ListItem',
        position: index + 1,
        name: item.name,
        item: absoluteUrl(item.path, origin),
      })),
    });
  }

  return {
    '@context': 'https://schema.org',
    '@graph': graph,
  };
}
