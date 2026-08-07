import React, { useEffect, useId, useRef, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';

const FIDELE_LINKS = [
  { to: '/demande', label: 'Faire une demande' },
  { to: '/suivi', label: 'Suivre une demande' },
  { to: '/horaires', label: 'Horaires des messes' },
];

const PARISH_LINKS = [
  { to: '/inscription-paroisse', label: 'Inscrire ma paroisse' },
  { to: '/admin/login', label: 'Espace paroisse' },
];

function NavDropdown({ label, links, open, onToggle, onNavigate, active }) {
  const panelId = useId();

  return (
    <div className={`nav-dropdown${open ? ' is-open' : ''}${active ? ' has-active' : ''}`}>
      <button
        type="button"
        className="nav-link nav-dropdown-trigger"
        aria-expanded={open}
        aria-haspopup="true"
        aria-controls={panelId}
        onClick={onToggle}
      >
        {label}
        <span className="nav-dropdown-chevron" aria-hidden="true" />
      </button>
      <div id={panelId} className="nav-dropdown-panel" role="menu" hidden={!open}>
        {links.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            role="menuitem"
            className={({ isActive }) => `nav-dropdown-item${isActive ? ' active' : ''}`}
            onClick={onNavigate}
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </div>
  );
}

export default function PublicHeader() {
  const { pathname } = useLocation();
  const navRef = useRef(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [openMenu, setOpenMenu] = useState(null);

  const closeAll = () => {
    setMenuOpen(false);
    setOpenMenu(null);
  };

  useEffect(() => {
    closeAll();
  }, [pathname]);

  useEffect(() => {
    if (!openMenu && !menuOpen) return undefined;

    const onPointerDown = (e) => {
      if (navRef.current && !navRef.current.contains(e.target)) {
        setOpenMenu(null);
      }
    };
    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        setOpenMenu(null);
        setMenuOpen(false);
      }
    };

    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [openMenu, menuOpen]);

  const fideleActive = FIDELE_LINKS.some((l) => pathname === l.to || pathname.startsWith(`${l.to}/`));
  const parishActive = PARISH_LINKS.some((l) => pathname === l.to || pathname.startsWith(`${l.to}/`));

  return (
    <header className="public-header">
      <div className="container public-bar">
        <Link to="/" className="brand" onClick={closeAll}>
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-text">
            <span className="brand-name">Missanye</span>
            <span className="brand-tag">Intentions &amp; célébrations</span>
          </span>
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-label={menuOpen ? 'Fermer le menu' : 'Ouvrir le menu'}
          aria-expanded={menuOpen}
          onClick={() => {
            setMenuOpen((v) => !v);
            setOpenMenu(null);
          }}
        >
          <span className={`nav-toggle-bars${menuOpen ? ' is-open' : ''}`} aria-hidden="true" />
        </button>

        <nav
          ref={navRef}
          className={`nav-links${menuOpen ? ' open' : ''}`}
          aria-label="Navigation principale"
        >
          <NavDropdown
            label="Fidèles"
            links={FIDELE_LINKS}
            open={openMenu === 'fideles'}
            active={fideleActive}
            onToggle={() => setOpenMenu((m) => (m === 'fideles' ? null : 'fideles'))}
            onNavigate={closeAll}
          />
          <NavDropdown
            label="Paroisses"
            links={PARISH_LINKS}
            open={openMenu === 'paroisses'}
            active={parishActive}
            onToggle={() => setOpenMenu((m) => (m === 'paroisses' ? null : 'paroisses'))}
            onNavigate={closeAll}
          />
        </nav>
      </div>
    </header>
  );
}
