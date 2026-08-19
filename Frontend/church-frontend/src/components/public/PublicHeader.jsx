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
  align = 'start',
}) {
  const panelId = useId();
  const leaveTimer = useRef(null);
  const triggerRef = useRef(null);
  const panelRef = useRef(null);

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
    }, 170);
  };

  const handleMouseEnter = () => {
    clearLeave();
    if (canHover()) onOpen?.();
  };

  const focusFirstLink = () => {
    window.requestAnimationFrame(() => {
      panelRef.current?.querySelector('a')?.focus();
    });
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Escape' && open) {
      event.preventDefault();
      event.stopPropagation();
      onClose?.();
      triggerRef.current?.focus();
      return;
    }

    if (event.currentTarget === event.target) return;

    if (
      event.target === triggerRef.current
      && (event.key === 'ArrowDown' || event.key === 'Enter')
      && !open
    ) {
      event.preventDefault();
      onOpen?.();
      focusFirstLink();
    }
  };

  const handleBlurCapture = (event) => {
    if (!event.currentTarget.contains(event.relatedTarget)) {
      onClose?.();
    }
  };

  useEffect(() => () => clearLeave(), []);

  return (
    <div
      className={`nav-dropdown nav-dropdown--rich nav-dropdown--align-${align}${open ? ' is-open' : ''}${active ? ' has-active' : ''}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={scheduleClose}
      onBlurCapture={handleBlurCapture}
      onKeyDown={handleKeyDown}
    >
      <button
        ref={triggerRef}
        type="button"
        className="nav-link nav-dropdown-trigger"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={onToggle}
      >
        {label}
        <span className="nav-dropdown-chevron" aria-hidden="true" />
      </button>

      <div
        ref={panelRef}
        id={panelId}
        className="nav-dropdown-panel nav-dropdown-panel--rich"
        hidden={!open}
      >
        <div className="nav-dropdown-intro">
          <span>{eyebrow}</span>
          <strong>{label}</strong>
          <p>{description}</p>
        </div>

        <ul className="nav-dropdown-menu" aria-label={`Liens ${label}`}>
          {links.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
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
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default function PublicHeader() {
  const { pathname } = useLocation();
  const navigationBoundaryRef = useRef(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [openMenu, setOpenMenu] = useState(null);
  const isHome = pathname === '/';

  const closeAll = () => {
    setMenuOpen(false);
    setOpenMenu(null);
  };

  useEffect(() => {
    setMenuOpen(false);
    setOpenMenu(null);
  }, [pathname]);

  useEffect(() => {
    if (!openMenu && !menuOpen) return undefined;

    const onPointerDown = (event) => {
      if (
        navigationBoundaryRef.current
        && !navigationBoundaryRef.current.contains(event.target)
      ) {
        setOpenMenu(null);
        setMenuOpen(false);
      }
    };

    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
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

  const fideleActive = FIDELE_LINKS.some(
    (link) => pathname === link.to || pathname.startsWith(`${link.to}/`)
  );
  const parishActive = PARISH_LINKS.some(
    (link) => pathname === link.to || pathname.startsWith(`${link.to}/`)
  );

  return (
    <header className={`public-header${isHome ? ' public-header--home' : ''}`}>
      <div ref={navigationBoundaryRef} className="container public-bar">
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
          aria-controls="public-main-navigation"
          onClick={() => {
            setMenuOpen((value) => !value);
            setOpenMenu(null);
          }}
        >
          <span className={`nav-toggle-bars${menuOpen ? ' is-open' : ''}`} aria-hidden="true" />
        </button>

        <nav
          id="public-main-navigation"
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
            onToggle={() => setOpenMenu((menu) => (menu === 'fideles' ? null : 'fideles'))}
            onClose={() => setOpenMenu((menu) => (menu === 'fideles' ? null : menu))}
            onNavigate={closeAll}
          />
          <NavDropdown
            label="Paroisses"
            eyebrow="Pour votre paroisse"
            description="Découvrir Missanye, inscrire votre paroisse ou accéder à son espace."
            links={PARISH_LINKS}
            open={openMenu === 'paroisses'}
            active={parishActive}
            align="end"
            onOpen={() => setOpenMenu('paroisses')}
            onToggle={() => setOpenMenu((menu) => (menu === 'paroisses' ? null : 'paroisses'))}
            onClose={() => setOpenMenu((menu) => (menu === 'paroisses' ? null : menu))}
            onNavigate={closeAll}
          />
          <ThemeToggle compact className="public-theme-toggle" />
        </nav>
      </div>
    </header>
  );
}
