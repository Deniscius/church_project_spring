import React, { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';

export default function PublicHeader() {
  const [menuOpen, setMenuOpen] = useState(false);

  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="public-header">
      <div className="container public-bar">
        <Link to="/" className="brand" onClick={closeMenu}>
          <span className="brand-mark" aria-hidden="true">✝</span>
          <span>Messes Paroissiales</span>
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-label={menuOpen ? 'Fermer le menu' : 'Ouvrir le menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((v) => !v)}
        >
          {menuOpen ? '✕' : '☰'}
        </button>

        <nav className={`nav-links${menuOpen ? ' open' : ''}`} aria-label="Navigation principale">
          <NavLink to="/demande" className="nav-link" onClick={closeMenu}>
            Faire une demande
          </NavLink>
          <NavLink to="/suivi" className="nav-link" onClick={closeMenu}>
            Suivre une demande
          </NavLink>
          <NavLink
            to="/admin/login"
            className={({ isActive }) => `nav-link nav-link-cta${isActive ? ' active' : ''}`}
            onClick={closeMenu}
          >
            Espace paroisse
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
