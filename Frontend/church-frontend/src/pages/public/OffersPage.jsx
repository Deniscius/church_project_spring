import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import AppAlert from '../../components/ui/AppAlert';
import { ROUTES } from '../../constants/routes';
import { planSaasService } from '../../services/planSaas.service';
import { formatCurrency } from '../../utils/formatCurrency';
import './OffersPage.css';

const PLATFORM_BENEFITS = [
  {
    title: 'Demandes centralisées',
    text: 'Recevez et suivez les demandes de messe dans un espace unique, sans multiplier les registres.',
  },
  {
    title: 'Paiements et reçus',
    text: 'Suivez les règlements et fournissez des reçus PDF personnalisés à la paroisse.',
  },
  {
    title: 'Horaires et intentions',
    text: 'Organisez les célébrations et préparez les feuilles d’intentions depuis le même outil.',
  },
  {
    title: 'Équipe paroissiale',
    text: 'Travaillez avec les responsables autorisés dans un espace sécurisé et rattaché à la paroisse.',
  },
  {
    title: 'Pilotage quotidien',
    text: 'Retrouvez les indicateurs utiles, le suivi administratif et la trésorerie sans ressaisie.',
  },
  {
    title: 'Données isolées',
    text: 'Chaque paroisse travaille dans son propre espace, séparé des données des autres paroisses.',
  },
];

const FAQ = [
  {
    question: 'Les fonctionnalités changent-elles selon la formule ?',
    answer:
      'Non. Les formules actuelles se distinguent par leur durée et leur prix. Vous choisissez surtout la période d’abonnement qui convient à votre paroisse.',
  },
  {
    question: 'Doit-on payer au moment de l’inscription ?',
    answer:
      'Non. Le dossier de la paroisse est d’abord transmis et vérifié. Le paiement intervient après validation, avant l’activation de l’espace paroisse.',
  },
  {
    question: 'Peut-on changer de formule ?',
    answer:
      'Oui. La formule peut être revue au renouvellement afin d’adapter la durée d’abonnement aux besoins de la paroisse.',
  },
  {
    question: 'Que se passe-t-il après l’inscription ?',
    answer:
      'Missanye vérifie les informations et les documents transmis. Après approbation et règlement, l’espace paroisse peut être activé pour l’équipe autorisée.',
  },
];

function durationLabel(months) {
  if (months === 1) return '1 mois';
  if (months === 12) return '1 an';
  return `${months} mois`;
}

function normalizePlan(plan) {
  const durationMonths = Math.max(Number(plan?.dureeMois) || 1, 1);
  const price = Math.max(Number(plan?.montantXof) || 0, 0);
  return {
    id: plan?.publicId || plan?.code,
    code: String(plan?.code || ''),
    name: plan?.nom || String(plan?.code || 'Formule'),
    description: plan?.description || '',
    price,
    durationMonths,
    monthlyEquivalent: Math.round(price / durationMonths),
    featured: Boolean(plan?.featured),
    displayOrder: Number(plan?.ordreAffichage) || 0,
  };
}

function savingsPercent(plan, monthlyReference) {
  if (!monthlyReference || monthlyReference.durationMonths !== 1 || plan.durationMonths <= 1) return 0;
  if (monthlyReference.price <= 0 || plan.price <= 0) return 0;
  const referenceTotal = monthlyReference.price * plan.durationMonths;
  if (plan.price >= referenceTotal) return 0;
  return Math.round(((referenceTotal - plan.price) / referenceTotal) * 100);
}

export default function OffersPage() {
  const {
    data,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['plans-saas', 'public', 'offers'],
    queryFn: planSaasService.listPublic,
    staleTime: 5 * 60_000,
  });

  const plans = useMemo(
    () => (Array.isArray(data) ? data : [])
      .map(normalizePlan)
      .filter((plan) => plan.code && plan.price > 0)
      .sort((a, b) => a.displayOrder - b.displayOrder),
    [data]
  );

  const monthlyReference = useMemo(
    () => plans.find((plan) => plan.durationMonths === 1) || null,
    [plans]
  );

  return (
    <div className="offers-page">
      <section className="offers-hero" aria-labelledby="offers-title">
        <div className="container offers-hero-inner">
          <div className="offers-hero-copy">
            <span className="offers-kicker">Missanye pour les paroisses</span>
            <h1 id="offers-title">Une gestion paroissiale plus simple, avec un prix clair</h1>
            <p>
              Centralisez les demandes de messe, les paiements, les reçus, les horaires et le suivi
              administratif dans un espace pensé pour votre paroisse.
            </p>
            <div className="button-row offers-hero-actions">
              <a className="btn btn-primary btn-lg" href="#formules">Voir les formules</a>
              <Link className="btn btn-secondary btn-lg" to={ROUTES.PARISH_REGISTRATION}>
                Inscrire ma paroisse
              </Link>
            </div>
            <div className="offers-trust-row" aria-label="Engagements de présentation">
              <span>Prix en FCFA</span>
              <span>Mêmes fonctionnalités</span>
              <span>Aucun paiement au dépôt du dossier</span>
            </div>
          </div>
          <aside className="offers-hero-card" aria-label="Ce que votre paroisse gagne">
            <span className="offers-hero-card-label">Au quotidien</span>
            <strong>Moins de dispersion. Plus de visibilité.</strong>
            <ul>
              <li>Un seul espace pour les demandes</li>
              <li>Des reçus et paiements mieux suivis</li>
              <li>Une équipe paroissiale mieux organisée</li>
            </ul>
          </aside>
        </div>
      </section>

      <main className="container offers-main">
        <section id="formules" className="offers-section" aria-labelledby="offers-pricing-title">
          <div className="offers-section-head">
            <span className="offers-kicker">Formules</span>
            <h2 id="offers-pricing-title">Choisissez seulement la durée qui vous convient</h2>
            <p>
              Les fonctionnalités de gestion restent les mêmes. La différence porte sur la durée
              d’abonnement et le montant correspondant.
            </p>
          </div>

          {isLoading ? (
            <div className="offers-loading" role="status">Chargement des formules…</div>
          ) : null}

          {isError ? (
            <AppAlert variant="danger">
              Impossible de charger les formules pour le moment.{' '}
              <button type="button" className="link-button" onClick={() => refetch()} disabled={isFetching}>
                Réessayer
              </button>
            </AppAlert>
          ) : null}

          {!isLoading && !isError && !plans.length ? (
            <AppAlert variant="info">
              Les formules d’abonnement sont temporairement indisponibles. Vous pouvez néanmoins
              transmettre le dossier de votre paroisse.
            </AppAlert>
          ) : null}

          {plans.length ? (
            <div className="offers-plan-grid">
              {plans.map((plan) => {
                const saving = savingsPercent(plan, monthlyReference);
                const registrationUrl = `${ROUTES.PARISH_REGISTRATION}?plan=${encodeURIComponent(plan.code)}`;
                return (
                  <article
                    key={plan.id || plan.code}
                    className={`offers-plan-card${plan.featured ? ' is-featured' : ''}`}
                  >
                    <div className="offers-plan-badges">
                      {plan.featured ? <span className="offers-plan-featured">Recommandée</span> : null}
                      {saving > 0 ? <span className="offers-plan-saving">Économisez {saving}%</span> : null}
                    </div>

                    <div className="offers-plan-heading">
                      <span className="offers-plan-duration">{durationLabel(plan.durationMonths)}</span>
                      <h3>{plan.name}</h3>
                      {plan.description ? <p>{plan.description}</p> : null}
                    </div>

                    <div className="offers-plan-price">
                      <strong>{formatCurrency(plan.price)}</strong>
                      <span>pour {durationLabel(plan.durationMonths)}</span>
                    </div>

                    <div className="offers-plan-monthly">
                      <span>Équivalent mensuel</span>
                      <strong>{formatCurrency(plan.monthlyEquivalent)} / mois</strong>
                    </div>

                    <ul className="offers-plan-included" aria-label={`Inclus dans ${plan.name}`}>
                      <li>Accès aux outils de gestion Missanye</li>
                      <li>Suivi des demandes et des paiements</li>
                      <li>Reçus, horaires et intentions</li>
                      <li>Espace sécurisé pour la paroisse</li>
                    </ul>

                    <div className="offers-plan-footer">
                      <Link
                        className={`btn ${plan.featured ? 'btn-primary' : 'btn-secondary'} offers-plan-cta`}
                        to={registrationUrl}
                      >
                        Choisir cette formule
                      </Link>
                      <small>Aucun paiement pendant le dépôt du dossier.</small>
                    </div>
                  </article>
                );
              })}
            </div>
          ) : null}
        </section>

        <section className="offers-section offers-benefits" aria-labelledby="offers-benefits-title">
          <div className="offers-section-head offers-section-head--center">
            <span className="offers-kicker">Tout Missanye</span>
            <h2 id="offers-benefits-title">Une formule, tout l’essentiel pour travailler</h2>
            <p>
              La plateforme accompagne le parcours depuis le dépôt d’une demande jusqu’au suivi
              paroissial, sans imposer plusieurs niveaux de fonctionnalités.
            </p>
          </div>
          <div className="offers-benefit-grid">
            {PLATFORM_BENEFITS.map((benefit) => (
              <article key={benefit.title} className="offers-benefit-card">
                <span className="offers-benefit-check" aria-hidden="true">✓</span>
                <h3>{benefit.title}</h3>
                <p>{benefit.text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="offers-decision-card" aria-labelledby="offers-decision-title">
          <div>
            <span className="offers-kicker">Prêt à démarrer ?</span>
            <h2 id="offers-decision-title">Déposez le dossier de votre paroisse</h2>
            <p>
              L’inscription se fait en plusieurs étapes guidées. Le dossier est vérifié avant toute
              activation et avant tout règlement.
            </p>
          </div>
          <div className="button-row">
            <Link className="btn btn-primary btn-lg" to={ROUTES.PARISH_REGISTRATION}>
              Inscrire ma paroisse
            </Link>
            <a className="btn btn-secondary btn-lg" href="#questions">Voir les questions fréquentes</a>
          </div>
        </section>

        <section id="questions" className="offers-section offers-faq" aria-labelledby="offers-faq-title">
          <div className="offers-section-head">
            <span className="offers-kicker">Questions fréquentes</span>
            <h2 id="offers-faq-title">Décider avec toutes les informations utiles</h2>
          </div>
          <div className="offers-faq-list">
            {FAQ.map((item) => (
              <details key={item.question} className="offers-faq-item">
                <summary>{item.question}</summary>
                <p>{item.answer}</p>
              </details>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
