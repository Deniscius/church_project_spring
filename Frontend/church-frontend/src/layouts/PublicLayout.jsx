import React from 'react';
import { Outlet } from 'react-router-dom';
import PublicHeader from '../components/public/PublicHeader';
import PublicFooter from '../components/public/PublicFooter';

export default function PublicLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#contenu-principal">
        Aller au contenu
      </a>
      <PublicHeader />
      <main id="contenu-principal" className="public-main">
        <Outlet />
      </main>
      <PublicFooter />
    </div>
  );
}
