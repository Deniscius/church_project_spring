import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

/**
 * Barre de progression fine en haut de l'écran pendant les navigations /
 * chargements de chunks lazy. Plus légère et lisible qu'un spinner plein écran.
 */
export default function RouteProgressBar() {
  const location = useLocation();
  const navType = useNavigationType();
  const routeKey = `${location.key}:${navType}`;
  const [visible, setVisible] = useState(false);
  const [width, setWidth] = useState(0);
  const timersRef = useRef([]);

  useEffect(() => {
    timersRef.current.forEach((id) => window.clearTimeout(id));
    setVisible(true);
    setWidth(12);
    timersRef.current = [];

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
  }, [routeKey]);

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
