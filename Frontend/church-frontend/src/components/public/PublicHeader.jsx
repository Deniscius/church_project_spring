import React, { useEffect, useId, useRef, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import ThemeToggle from '../ui/ThemeToggle';
import BrandLogo from '../ui/BrandLogo';

const FIDELE_LINKS = [
  { to: '/demande', label: 'Faire une demande' },
  { to: '/suivi', label: 'Suivre une demande' },
  { to: '/horaires', label: 'Horaires des messes' },
];

const PARISH_LINKS = [
  { to: '/inscription-paroisse', label: 'Inscrire ma paroisse' },
  { to: '/admin/login', label: 'Espace paroisse' },
];

function NavDropdown({
  label,
  links,
  open,
  onToggle,
  onNavigate,
  onClose,
  active,
}) {
  const panelId = useId();
  const leaveTimer = useRef(null);

  const canHoverClose = () =>
    typeof window !== 'undefined'
    && window.matchMedia('(hover: hover) and (pointer: fine)').matches;

  const clearLeave = () => {
    if (leaveTimer.current) {
      window.clearTimeout(leaveTimer.current);
      leaveTimer.current = null;
    }
  };

  const scheduleClose = () => {
    if (!canHoverClose()) return;
    clearLeave();
    leaveTimer.current = window.setTimeout(() => {
      onClose?.();
    }, 140);
  };

  useEffect(() => () => clearLeave(), []);

  return (
    <div
      className={`nav-dropdown${open ? ' is-open' : ''}${active ? ' has-active' : ''}`}
      onMouseEnter={clearLeave}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        className="nav-link nav-dropdown-trigger"
        aria-expanded={open}
        aria-haspopup="true"
        aria-controls={panelId}
        onClick={onToggle}
        onBlur={(e) => {
          // Ferme si le focus quitte le menu (évite état « hover » figé au clavier / tactile).
          if (!e.currentTarget.parentElement?.contains(e.relatedTarget)) {
            onClose?.();
          }
        }}
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
  const isHome = pathname === '/';

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
    <header className={`public-header${isHome ? ' public-header--home' : ''}`}>
      <div className="container public-bar">
        <Link to="/" className="brand" onClick={closeAll}>
          <BrandLogo size={34} className="public-brand-logo" alt="" />
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
            onClose={() => setOpenMenu((m) => (m === 'fideles' ? null : m))}
            onNavigate={closeAll}
          />
          <NavDropdown
            label="Paroisses"
            links={PARISH_LINKS}
            open={openMenu === 'paroisses'}
            active={parishActive}
            onToggle={() => setOpenMenu((m) => (m === 'paroisses' ? null : 'paroisses'))}
            onClose={() => setOpenMenu((m) => (m === 'paroisses' ? null : m))}
            onNavigate={closeAll}
          />
          <ThemeToggle compact className="public-theme-toggle" />
        </nav>
      </div>
    </header>
  );
}
