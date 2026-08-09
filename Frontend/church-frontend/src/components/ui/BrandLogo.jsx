import React, { useState } from 'react';

/**
 * Logo Missanye : utilise `/assets/logo-missanye.png` (ou .svg) dès qu’il est déposé.
 * Sinon, marque de secours (pastille) pour ne pas casser l’UI.
 *
 * @param {{ size?: number, className?: string, alt?: string }} props
 */
export default function BrandLogo({ size = 32, className = '', alt = 'Missanye' }) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <span
        className={`brand-mark brand-logo-fallback ${className}`.trim()}
        aria-hidden={alt ? undefined : true}
        title={alt}
      />
    );
  }

  return (
    <img
      className={`brand-logo ${className}`.trim()}
      src="/assets/logo-missanye.png"
      alt={alt}
      width={size}
      height={size}
      decoding="async"
      onError={() => setFailed(true)}
    />
  );
}
