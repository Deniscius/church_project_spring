import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { buildPageJsonLd } from '../../seo/jsonLd';
import {
  SITE_DEFAULT_DESCRIPTION,
  SITE_IMAGE_PATH,
  SITE_NAME,
  SITE_OG_IMAGE_ALT,
  SITE_OG_IMAGE_HEIGHT,
  SITE_OG_IMAGE_WIDTH,
  SITE_ORIGIN,
  absoluteUrl,
  resolveRouteMeta,
} from '../../seo/siteMeta';

function upsertMeta(attr, key, content) {
  if (content == null) return;
  let el = document.head.querySelector(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(attr, key);
    document.head.appendChild(el);
  }
  el.setAttribute('content', content);
}

function upsertLink(rel, href) {
  if (!href) return;
  let el = document.head.querySelector(`link[rel="${rel}"]`);
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', rel);
    document.head.appendChild(el);
  }
  el.setAttribute('href', href);
}

/**
 * Met à jour title / description / OG / robots / JSON-LD à chaque navigation.
 */
export default function DocumentMeta() {
  const { pathname } = useLocation();

  useEffect(() => {
    const meta = resolveRouteMeta(pathname);
    const origin = typeof window !== 'undefined' ? window.location.origin : SITE_ORIGIN;
    const url = absoluteUrl(pathname, origin);
    const image = absoluteUrl(SITE_IMAGE_PATH, origin);
    const indexable = !String(meta.robots || '').includes('noindex');

    document.title = meta.title;
    upsertMeta('name', 'description', meta.description);
    upsertMeta('name', 'robots', meta.robots);
    upsertMeta('name', 'author', SITE_NAME);
    upsertMeta('property', 'og:type', meta.ogType || 'website');
    upsertMeta('property', 'og:site_name', SITE_NAME);
    upsertMeta('property', 'og:title', meta.title);
    upsertMeta('property', 'og:description', meta.description || SITE_DEFAULT_DESCRIPTION);
    upsertMeta('property', 'og:url', url);
    upsertMeta('property', 'og:locale', 'fr_FR');
    upsertMeta('property', 'og:image', image);
    upsertMeta('property', 'og:image:type', 'image/jpeg');
    upsertMeta('property', 'og:image:width', SITE_OG_IMAGE_WIDTH);
    upsertMeta('property', 'og:image:height', SITE_OG_IMAGE_HEIGHT);
    upsertMeta('property', 'og:image:alt', SITE_OG_IMAGE_ALT);
    upsertMeta('name', 'twitter:card', 'summary_large_image');
    upsertMeta('name', 'twitter:title', meta.title);
    upsertMeta('name', 'twitter:description', meta.description || SITE_DEFAULT_DESCRIPTION);
    upsertMeta('name', 'twitter:image', image);
    upsertMeta('name', 'twitter:image:alt', SITE_OG_IMAGE_ALT);

    if (indexable) {
      upsertLink('canonical', url);
    } else {
      const canonical = document.head.querySelector('link[rel="canonical"]');
      if (canonical) canonical.remove();
    }

    const scriptId = 'site-jsonld';
    let script = document.getElementById(scriptId);
    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.type = 'application/ld+json';
      document.head.appendChild(script);
    }
    script.textContent = JSON.stringify(
      buildPageJsonLd({
        pathname,
        title: meta.title,
        description: meta.description,
        origin,
        image,
      })
    );
  }, [pathname]);

  return null;
}
