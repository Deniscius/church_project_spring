import React from 'react';
import { Link } from 'react-router-dom';

export default function PublicHeader() {
  return (
    <header className="public-header">
      <div className="container public-bar">
        <Link to="/" style={{ fontWeight: 800 }}>Church Requests</Link>
        <nav className="nav-links">
          <Link to="/demande">Faire une demande</Link>
          <Link to="/suivi">Suivre une demande</Link>
          <Link to="/admin/login">Espace paroisse</Link>
        </nav>
      </div>
    </header>
  );
}
