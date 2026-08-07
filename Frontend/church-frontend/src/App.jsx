import React from 'react';
import { AppProvider } from './app/providers/AppProvider';
import AppRouter from './app/router';
import RouteProgressBar from './components/ui/RouteProgressBar';
import DocumentMeta from './components/seo/DocumentMeta';
import ErrorBoundary from './components/ui/ErrorBoundary';

export default function App() {
  return (
    <ErrorBoundary>
      <AppProvider>
        <DocumentMeta />
        <RouteProgressBar />
        <AppRouter />
      </AppProvider>
    </ErrorBoundary>
  );
}

