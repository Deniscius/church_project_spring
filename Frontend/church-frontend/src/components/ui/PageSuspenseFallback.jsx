import React from 'react';
import AppLoading from './AppLoading';

/** Fallback Suspense unifié (lazy routes). */
export default function PageSuspenseFallback({ message = 'Chargement de la page…' }) {
  return (
    <div className="page-suspense-fallback">
      <AppLoading message={message} size="lg" />
    </div>
  );
}
