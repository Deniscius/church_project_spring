import React from 'react';
import { Link } from 'react-router-dom';

export default function PublicFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="public-footer">
      <div className="container footer-grid">
        <div>
          <div className="footer-brand">Messes Paroissiales</div>
          <p style={{ color: 'rgba(241,245,249,0.6)', fontSize: 14, lineHeight: 1.6, margin: 0, maxWidth: 320 }}>
            Plateforme de gestion des demandes de messes pour les paroisses.
            Déposez, suivez et gérez vos intentions en toute simplicité.
          </p>
        </div>
        <div>
          <div className="footer-links-title">Fidèles</div>
          <div className="footer-links">
            <Link to="/demande">Faire une demande</Link>
            <Link to="/suivi">Suivre une demande</Link>
          </div>
        </div>
        <div>
          <div className="footer-links-title">Administration</div>
          <div className="footer-links">
            <Link to="/admin/login">Connexion paroisse</Link>
          </div>
        </div>
      </div>
      <div className="container footer-bottom">
        <span>© {year} Messes Paroissiales</span>
        <span>Système multi-paroisses sécurisé</span>
      </div>
    </footer>
  );
}
