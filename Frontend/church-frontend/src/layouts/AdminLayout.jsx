import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/admin/Sidebar';
import Topbar from '../components/admin/Topbar';
import SubscriptionBanner from '../components/admin/SubscriptionBanner';
import TenantInterventionBanner from '../components/admin/TenantInterventionBanner';

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <Sidebar />
      <div className="admin-main">
        <Topbar />
        <div className="page-section">
          <TenantInterventionBanner />
          <SubscriptionBanner />
          <Outlet />
        </div>
      </div>
    </div>
  );
}
