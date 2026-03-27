import React from 'react';

export default function PublicFooter() {
  return (
    <footer className="public-footer">
      <div className="container public-bar">
        <span>Blueprint frontend pour le système de gestion des demandes de messes</span>
        <span style={{ color: 'var(--muted)' }}>Public + Admin paroisse dans une seule app</span>
      </div>
    </footer>
  );
}
