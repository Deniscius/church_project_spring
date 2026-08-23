import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/admin/Sidebar';
import Topbar from '../components/admin/Topbar';
import SubscriptionBanner from '../components/admin/SubscriptionBanner';
import TenantInterventionBanner from '../components/admin/TenantInterventionBanner';

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <a className="skip-link" href="#contenu-principal-admin">
        Aller au contenu
      </a>
      <Sidebar />
      <div className="admin-main">
        <Topbar />
        <main id="contenu-principal-admin" className="page-section">
          <TenantInterventionBanner />
          <SubscriptionBanner />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
