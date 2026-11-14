import React from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';

const RELEASES = 'https://github.com/Deniscius/church_project_spring/releases/latest';

export default function DesktopDownloadPage() {
  return (
    <div className="container stack desktop-download-page">
      <PageHeader title="Missanye sur votre ordinateur" subtitle="L’espace de votre paroisse, disponible sur Windows même lorsque la connexion s’interrompt." />
      <section className="desktop-download-card" aria-labelledby="desktop-download-title">
        <div className="desktop-download-icon" aria-hidden="true">↓</div>
        <div>
          <p className="desktop-download-eyebrow">APPLICATION WINDOWS · 64 BITS</p>
          <h2 id="desktop-download-title">Télécharger Missanye Desktop</h2>
          <p>Installez la dernière version publiée pour gérer les demandes, les célébrations et les paramètres de votre paroisse.</p>
          <div className="button-row">
            <a className="btn btn-primary btn-lg" href={`${RELEASES}/download/Missanye-Setup.exe`}>Télécharger pour Windows (.exe)</a>
            <a className="btn btn-secondary" href={RELEASES} target="_blank" rel="noopener noreferrer">Voir les nouveautés</a>
          </div>
          <p className="muted">Ce bouton télécharge toujours la dernière version disponible, y compris après une mise à jour.</p>
        </div>
      </section>
      <section aria-labelledby="desktop-install-title">
        <h2 id="desktop-install-title">Prêt en trois étapes</h2>
        <ol className="home-steps">
          <li><span className="home-step-num" aria-hidden="true">1</span><div><h3>Installer</h3><p>Ouvrez le fichier téléchargé et autorisez l’installation dans Program Files avec les droits administrateur.</p></div></li>
          <li><span className="home-step-num" aria-hidden="true">2</span><div><h3>Connecter votre paroisse</h3><p>Une première connexion Internet et votre compte paroissial sont nécessaires pour préparer le poste.</p></div></li>
          <li><span className="home-step-num" aria-hidden="true">3</span><div><h3>Continuer à travailler</h3><p>Les données se synchronisent lorsque le service est accessible. Vos opérations hors connexion restent conservées sur le poste.</p></div></li>
        </ol>
      </section>
      <section className="desktop-update-note" aria-labelledby="desktop-update-title">
        <h2 id="desktop-update-title">Déjà installé ?</h2>
        <p>Dans l’application, ouvrez <strong>Paramètres → Application et mises à jour → Rechercher une mise à jour</strong>. Vous pouvez aussi télécharger le nouvel installateur ici et le lancer pour mettre votre version à jour.</p>
        <Link to="/admin/login">Accéder à l’espace paroisse sur le web →</Link>
      </section>
    </div>
  );
}
