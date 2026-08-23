import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

/**
 * Un cycle est remonté à chaque changement de route grâce à sa key.
 * Cela réinitialise proprement la progression sans modifier l'état pendant le rendu.
 */
function RouteProgressCycle() {
  const [visible, setVisible] = useState(true);
  const [width, setWidth] = useState(12);
  const timersRef = useRef([]);

  useEffect(() => {
    const t1 = window.setTimeout(() => setWidth(55), 80);
    const t2 = window.setTimeout(() => setWidth(78), 220);
    const t3 = window.setTimeout(() => setWidth(92), 480);
    const t4 = window.setTimeout(() => {
      setWidth(100);
      const hide = window.setTimeout(() => {
        setVisible(false);
        setWidth(0);
      }, 180);
      timersRef.current.push(hide);
    }, 700);

    timersRef.current.push(t1, t2, t3, t4);
    return () => {
      timersRef.current.forEach((id) => window.clearTimeout(id));
      timersRef.current = [];
    };
  }, []);

  if (!visible && width === 0) return null;

  return (
    <div
      className={`route-progress${visible ? ' is-visible' : ''}`}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(width)}
      aria-label="Chargement de la page"
    >
      <div className="route-progress-bar" style={{ width: `${width}%` }} />
    </div>
  );
}

/**
 * Barre de progression fine en haut de l'écran pendant les changements de route.
 */
export default function RouteProgressBar() {
  const location = useLocation();
  const navType = useNavigationType();
  const routeKey = `${location.key}:${navType}`;

  return <RouteProgressCycle key={routeKey} />;
}
