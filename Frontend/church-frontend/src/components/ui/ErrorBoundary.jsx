import React, { Component } from 'react';
import AppAlert from './AppAlert';
import AppButton from './AppButton';

/**
 * Filet de sécurité UI : une erreur de rendu n'écrase plus toute l'application.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error) {
    return {
      hasError: true,
      message: error?.message || 'Une erreur inattendue est survenue.',
    };
  }

  componentDidCatch(error, info) {
    if (import.meta.env.DEV) {
      console.error('ErrorBoundary', error, info?.componentStack);
    }
  }

  handleRetry = () => {
    this.setState({ hasError: false, message: '' });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="page-section" style={{ maxWidth: 640, margin: '40px auto' }}>
          <AppAlert variant="danger">
            <strong>Impossible d’afficher cette page</strong>
            <p style={{ margin: '8px 0 0' }}>{this.state.message}</p>
          </AppAlert>
          <div className="button-row" style={{ marginTop: 16 }}>
            <AppButton type="button" onClick={this.handleRetry}>
              Réessayer
            </AppButton>
            <AppButton type="button" variant="secondary" onClick={() => { window.location.href = '/'; }}>
              Retour à l’accueil
            </AppButton>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
