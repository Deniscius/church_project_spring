import React, { useState } from 'react';
import { Link, Outlet } from 'react-router-dom';

export default function AuthLayout() {
  const [illustrationFailed, setIllustrationFailed] = useState(false);

  return (
    <div className="auth-shell">
      <aside className="auth-panel-brand" aria-label="Présentation">
        <div className="auth-brand-top">
          <Link to="/" className="auth-brand-link">
            <span className="brand-mark" aria-hidden="true" />
            <span className="auth-brand-name">Missanye</span>
          </Link>
        </div>

        <div className="auth-illustration-wrap">
          {illustrationFailed ? null : (
            <img
              className="auth-illustration"
              src="/assets/undraw-authentication.svg"
              alt=""
              width={520}
              height={407}
              decoding="async"
              loading="lazy"
              fetchPriority="low"
              onError={() => setIllustrationFailed(true)}
            />
          )}
        </div>

        <div className="auth-brand-copy">
          <p className="auth-brand-headline">Espace sécurisé de votre paroisse</p>
          <p className="auth-brand-tagline">
            Gérez les intentions, les célébrations et la trésorerie depuis un seul accès.
          </p>
        </div>
      </aside>

      <div className="auth-panel-form">
        <div className="auth-mobile-brand">
          <Link to="/" className="auth-brand-link auth-brand-link--light">
            <span className="brand-mark" aria-hidden="true" />
            <span className="auth-brand-name">Missanye</span>
          </Link>
        </div>
        <div className="auth-card">
          <Outlet />
        </div>
        <p className="auth-footer-note">
          Besoin d’aide ? Contactez l’administrateur de votre paroisse.
        </p>
      </div>
    </div>
  );
}
