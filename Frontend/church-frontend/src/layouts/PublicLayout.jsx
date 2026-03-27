import React from 'react';
import { Outlet } from 'react-router-dom';
import PublicHeader from '../components/public/PublicHeader';
import PublicFooter from '../components/public/PublicFooter';

export default function PublicLayout() {
  return (
    <div className="app-shell">
      <PublicHeader />
      <main className="page-section">
        <div className="container">
          <Outlet />
        </div>
      </main>
      <PublicFooter />
    </div>
  );
}
