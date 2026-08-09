import React from 'react';
import { Link } from 'react-router-dom';
import HomeWeekSchedules from '../../components/public/HomeWeekSchedules';

export default function HomePage() {
  return (
    <div className="home-page">
      <section className="home-hero" aria-labelledby="home-hero-title">
        <div className="home-hero-media" aria-hidden="true">
          <img
            src="/assets/hero-sanctuary.svg"
            alt=""
            width={1200}
            height={800}
            decoding="async"
            fetchPriority="high"
          />
        </div>
        <div className="home-hero-veil" aria-hidden="true" />
        <div className="container home-hero-content">
          <p className="home-hero-brand">Missanye</p>
          <h1 id="home-hero-title">Déposez une intention de messe en quelques minutes</h1>
          <p className="home-hero-lead">
            Choisissez votre paroisse, indiquez votre intention, puis suivez l’avancement avec un
            code unique.
          </p>
          <div className="home-hero-actions">
            <Link className="btn btn-primary btn-lg" to="/demande">
              Faire une demande
            </Link>
            <Link className="btn btn-ghost-light btn-lg" to="/suivi">
              Suivre une demande
            </Link>
          </div>
        </div>
      </section>

      <section className="home-section container" aria-labelledby="home-path-title">
        <div className="home-section-head">
          <h2 id="home-path-title">Un parcours simple</h2>
          <p className="muted">Trois étapes, sans créer de compte.</p>
        </div>
        <ol className="home-steps">
          <li>
            <span className="home-step-num" aria-hidden="true">1</span>
            <div>
              <h3>Déposer</h3>
              <p>Intention, lieu & date, puis paiement — 3 étapes.</p>
            </div>
          </li>
          <li>
            <span className="home-step-num" aria-hidden="true">2</span>
            <div>
              <h3>Recevoir un code</h3>
              <p>Conservez le code de suivi de votre intention.</p>
            </div>
          </li>
          <li>
            <span className="home-step-num" aria-hidden="true">3</span>
            <div>
              <h3>Suivre</h3>
              <p>Validation, paiement et prochaines étapes.</p>
            </div>
          </li>
        </ol>
      </section>

      <HomeWeekSchedules />

      <section className="home-section home-section-parish container" aria-labelledby="home-parish-title">
        <div className="home-parish-cta">
          <div>
            <h2 id="home-parish-title">Vous représentez une paroisse&nbsp;?</h2>
            <p className="muted">
              Gérez les demandes, les horaires et la trésorerie dans un espace dédié.
            </p>
          </div>
          <div className="button-row">
            <Link className="btn btn-primary" to="/inscription-paroisse">
              Inscrire ma paroisse
            </Link>
            <Link className="btn btn-secondary" to="/admin/login">
              Se connecter
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
