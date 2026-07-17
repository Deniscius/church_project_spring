import React from 'react';
import { Link, Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div className="auth-shell">
      <aside className="auth-panel-brand" aria-label="Présentation">
        <Link to="/" className="brand" style={{ color: 'white' }}>
          <span className="brand-mark" aria-hidden="true">✝</span>
          <span>Messes Paroissiales</span>
        </Link>
        <h2>Espace administration paroissiale</h2>
        <p>
          Connectez-vous pour gérer les demandes de messes, les paiements et le référentiel
          de votre paroisse en toute sécurité.
        </p>
        <div className="auth-panel-features">
          <div className="auth-feature">
            <span className="auth-feature-icon" aria-hidden="true">🔒</span>
            <span>Accès sécurisé par rôle et par paroisse</span>
          </div>
          <div className="auth-feature">
            <span className="auth-feature-icon" aria-hidden="true">📋</span>
            <span>Suivi des demandes et validations</span>
          </div>
          <div className="auth-feature">
            <span className="auth-feature-icon" aria-hidden="true">⛪</span>
            <span>Isolation multi-paroisses garantie</span>
          </div>
        </div>
      </aside>
      <div className="auth-panel-form">
        <div className="auth-card card">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
