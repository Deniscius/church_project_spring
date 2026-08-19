import React, { useEffect, useId, useRef, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import ThemeToggle from '../ui/ThemeToggle';
import BrandLogo from '../ui/BrandLogo';
import './PublicHeader.css';

const FIDELE_LINKS = [
  {
    to: '/demande',
    label: 'Faire une demande',
    description: 'Déposer une intention de messe en quelques étapes.',
  },
  {
    to: '/suivi',
    label: 'Suivre une demande',
    description: 'Consulter son statut avec votre code de suivi.',
  },
  {
    to: '/horaires',
    label: 'Horaires des messes',
    description: 'Voir les célébrations disponibles par paroisse.',
  },
];

const PARISH_LINKS = [
  {
    to: '/offres',
    label: 'Offres & tarifs',
    description: 'Comparer les formules, durées et prix Missanye.',
    featured: true,
  },
  {
    to: '/inscription-paroisse',
    label: 'Inscrire ma paroisse',
    description: 'Déposer un dossier d’inscription guidé.',
  },
  {
    to: '/admin/login',
    label: 'Espace paroisse',
    description: 'Accéder à la gestion de votre paroisse.',
  },
];

function NavDropdown({
  label,
  eyebrow,
  description,
  links,
  open,
  onOpen,
  onToggle,
  onNavigate,
  onClose,
  active,
}) {
  const panelId = useId();
  const leaveTimer = useRef(null);

  const canHover = () =>
    typeof window !== 'undefined'
    && window.matchMedia('(hover: hover) and (pointer: fine)').matches;

  const clearLeave = () => {
    if (leaveTimer.current) {
      window.clearTimeout(leaveTimer.current);
      leaveTimer.current = null;
    }
  };

  const scheduleClose = () => {
    if (!canHover()) return;
    clearLeave();
    leaveTimer.current = window.setTimeout(() => {
      onClose?.();
    }, 160);
  };

  const handleMouseEnter = () => {
    clearLeave();
    if (canHover()) onOpen?.();
  };

  useEffect(() => () => clearLeave(), []);

  return (
    <div
      className={`nav-dropdown nav-dropdown--rich${open ? ' is-open' : ''}${active ? ' has-active' : ''}`}
      onMouseEnter={handleMouseEnter}
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
          if (!e.currentTarget.parentElement?.contains(e.relatedTarget)) {
            onClose?.();
          }
        }}
      >
        {label}
        <span className="nav-dropdown-chevron" aria-hidden="true" />
      </button>

      <div id={panelId} className="nav-dropdown-panel nav-dropdown-panel--rich" hidden={!open}>
        <div className="nav-dropdown-intro" aria-hidden="true">
          <span>{eyebrow}</span>
          <strong>{label}</strong>
          <p>{description}</p>
        </div>

        <div className="nav-dropdown-menu" role="menu" aria-label={`Menu ${label}`}>
          {links.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              role="menuitem"
              className={({ isActive }) =>
                `nav-dropdown-item nav-dropdown-item--rich${isActive ? ' active' : ''}${item.featured ? ' is-featured' : ''}`
              }
              onClick={onNavigate}
            >
              <span className="nav-dropdown-item-copy">
                <strong>{item.label}</strong>
                <small>{item.description}</small>
              </span>
              <span className="nav-dropdown-item-arrow" aria-hidden="true">→</span>
            </NavLink>
          ))}
        </div>
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

  const [navPathname, setNavPathname] = useState(pathname);
  if (pathname !== navPathname) {
    setNavPathname(pathname);
    setMenuOpen(false);
    setOpenMenu(null);
  }

  const closeAll = () => {
    setMenuOpen(false);
    setOpenMenu(null);
  };

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
            eyebrow="Accès rapides"
            description="Les démarches utiles avant et après votre demande de messe."
            links={FIDELE_LINKS}
            open={openMenu === 'fideles'}
            active={fideleActive}
            onOpen={() => setOpenMenu('fideles')}
            onToggle={() => setOpenMenu((m) => (m === 'fideles' ? null : 'fideles'))}
            onClose={() => setOpenMenu((m) => (m === 'fideles' ? null : m))}
            onNavigate={closeAll}
          />
          <NavDropdown
            label="Paroisses"
            eyebrow="Pour votre paroisse"
            description="Découvrir Missanye, inscrire votre paroisse ou accéder à son espace."
            links={PARISH_LINKS}
            open={openMenu === 'paroisses'}
            active={parishActive}
            onOpen={() => setOpenMenu('paroisses')}
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
