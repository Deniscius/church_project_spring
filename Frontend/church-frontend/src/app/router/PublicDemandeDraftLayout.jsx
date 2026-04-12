import React from 'react';
import { Outlet } from 'react-router-dom';
import { PublicDemandeDraftProvider } from '../../contexts/publicDemandeDraft.context';

export default function PublicDemandeDraftLayout() {
  return (
    <PublicDemandeDraftProvider>
      <Outlet />
    </PublicDemandeDraftProvider>
  );
}
