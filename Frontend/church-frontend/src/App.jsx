import React from 'react';
import { AppProvider } from './app/providers/AppProvider';
import AppRouter from './app/router';

export default function App() {
  return (
    <AppProvider>
      <AppRouter />
    </AppProvider>
  );
}
