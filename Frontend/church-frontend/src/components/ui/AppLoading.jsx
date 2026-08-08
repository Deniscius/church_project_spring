import React from 'react';

export default function AppLoading({ message = 'Chargement…', size = 'default' }) {
  const spinnerClass = size === 'lg' ? 'spinner spinner-lg' : 'spinner';
  return (
    <div className="loading-center" role="status" aria-live="polite">
      <div className={spinnerClass} aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}
