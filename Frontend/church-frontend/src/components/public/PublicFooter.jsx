import React from 'react';
import { Link } from 'react-router-dom';

export default function PublicFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="public-footer">
      <div className="container footer-grid">
        <div>
          <div className="footer-brand">Missanye</div>
          <p className="footer-lead">
            Déposez une intention, suivez votre demande et consultez les horaires des paroisses
            partenaires — en toute simplicité.
          </p>
        </div>
        <div>
          <div className="footer-links-title">Fidèles</div>
          <div className="footer-links">
            <Link to="/demande">Faire une demande</Link>
            <Link to="/suivi">Suivre une demande</Link>
            <Link to="/horaires">Horaires des messes</Link>
          </div>
        </div>
        <div>
          <div className="footer-links-title">Paroisses</div>
          <div className="footer-links">
            <Link to="/inscription-paroisse">Inscrire ma paroisse</Link>
            <Link to="/admin/login">Connexion</Link>
            <Link to="/plan-du-site">Plan du site</Link>
          </div>
        </div>
      </div>
      <div className="container footer-bottom">
        <span>© {year} Missanye · www.missanye.com</span>
        <span>Multi-paroisses · Données isolées par paroisse</span>
      </div>
    </footer>
  );
}
