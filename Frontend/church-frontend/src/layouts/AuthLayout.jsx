import React, { useState } from 'react';
import { Link, Outlet } from 'react-router-dom';
import ThemeToggle from '../components/ui/ThemeToggle';
import BrandLogo from '../components/ui/BrandLogo';

export default function AuthLayout() {
  const [illustrationFailed, setIllustrationFailed] = useState(false);

  return (
    <div className="auth-shell">
      <aside className="auth-panel-brand" aria-label="Présentation Missanye">
        <div className="auth-brand-top">
          <Link to="/" className="auth-brand-link">
            <BrandLogo size={34} className="auth-brand-logo" alt="" />
            <span className="auth-brand-name">Missanye</span>
          </Link>
          <ThemeToggle compact className="auth-theme-toggle auth-theme-toggle--brand" />
        </div>

        <div className="auth-illustration-wrap">
          {illustrationFailed ? null : (
            <img
              className="auth-illustration"
              src="/assets/undraw-authentication.svg"
              alt=""
              width={480}
              height={376}
              decoding="async"
              loading="lazy"
              fetchPriority="low"
              onError={() => setIllustrationFailed(true)}
            />
          )}
        </div>

        <div className="auth-brand-copy">
          <p className="auth-brand-headline">L’espace de votre paroisse</p>
          <p className="auth-brand-tagline">
            Intentions, horaires et trésorerie — un accès sécurisé pour l’équipe paroissiale.
          </p>
        </div>
      </aside>

      <div className="auth-panel-form">
        <div className="auth-mobile-brand">
          <Link to="/" className="auth-brand-link auth-brand-link--light">
            <BrandLogo size={30} className="auth-brand-logo" alt="" />
            <span className="auth-brand-name">Missanye</span>
          </Link>
          <ThemeToggle compact className="auth-theme-toggle" />
        </div>
        <div className="auth-card">
          <Outlet />
        </div>
        <p className="auth-footer-note">
          Identifiants fournis par votre paroisse ou l’équipe Missanye.
        </p>
      </div>
    </div>
  );
}
