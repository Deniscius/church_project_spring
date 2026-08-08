import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/admin/Sidebar';
import Topbar from '../components/admin/Topbar';
import SubscriptionBanner from '../components/admin/SubscriptionBanner';

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <Sidebar />
      <div className="admin-main">
        <Topbar />
        <div className="page-section">
          <SubscriptionBanner />
          <Outlet />
        </div>
      </div>
    </div>
  );
}
