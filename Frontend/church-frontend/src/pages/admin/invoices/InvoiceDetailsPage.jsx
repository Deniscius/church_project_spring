import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppBadge from '../../../components/ui/AppBadge';
import { invoiceService } from '../../../services/invoice.service';
import { formatCurrency } from '../../../utils/formatCurrency';
import { formatFideleName } from '../../../utils/personName';
import { formatDate } from '../../../utils/formatDate';
import { paymentStatusLabel } from '../../../utils/statusMapper';

function Row({ label, children }) {
  return (
    <div className="info-row">
      <span>{label}</span>
      <span>{children ?? '—'}</span>
    </div>
  );
}

export default function InvoiceDetailsPage() {
  const { id } = useParams();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!id) return undefined;
    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await invoiceService.getById(id);
        if (!cancelled) setInvoice(data);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const reglement = invoice?.reglement || null;
  const montant = invoice?.montant != null ? Number(invoice.montant) : 0;
  const paye = invoice?.statutPaiement === 'PAYE';
  const demandeur = invoice
    ? formatFideleName(invoice.prenomFidele, invoice.nomFidele)
    : '';

  return (
    <div className="stack">
      <PageHeader
        title={invoice?.refFacture ? `Facture ${invoice.refFacture}` : 'Détail facture'}
        subtitle="Montant facturé, règlement de l’agrégateur et part revenant à la paroisse."
        actions={
          invoice?.demandePublicId ? (
            <Link className="btn btn-secondary" to={`/admin/demandes/${invoice.demandePublicId}`}>
              Voir la demande
            </Link>
          ) : null
        }
      />

      {loading ? <p className="muted">Chargement…</p> : null}
      {error ? <p className="alert-error">{error}</p> : null}

      {invoice ? (
        <>
          <section className="invoice-hero panel">
            <div>
              <p className="treasury-kicker">Montant facturé</p>
              <p className="treasury-balance">{formatCurrency(montant)}</p>
              <AppBadge
                value={invoice.statutPaiement}
                label={paymentStatusLabel(invoice.statutPaiement)}
              />
            </div>
            <div className="invoice-hero-meta">
              <div className="treasury-stat">
                <span>Émise le</span>
                <strong>{formatDate(invoice.dateEmission) || '—'}</strong>
              </div>
              <div className="treasury-stat">
                <span>Réglée le</span>
                <strong>{invoice.datePaiement ? formatDate(invoice.datePaiement) : '—'}</strong>
              </div>
              <div className="treasury-stat">
                <span>Net paroisse</span>
                <strong>
                  {reglement?.montantNet != null ? formatCurrency(Number(reglement.montantNet)) : '—'}
                </strong>
              </div>
            </div>
          </section>

          <div className="card-grid">
            <AppCard title="Objet de la facture">
              <div className="info-list">
                <Row label="Type de demande">{invoice.typeDemandeLibelle}</Row>
                <Row label="Forfait">{invoice.forfaitNom}</Row>
                <Row label="Intention">{invoice.intention}</Row>
                <Row label="Code de suivi">{invoice.codeSuivieDemande}</Row>
                <Row label="Statut de la demande">
                  {invoice.statutDemande ? <AppBadge value={invoice.statutDemande} /> : null}
                </Row>
              </div>
            </AppCard>

            <AppCard title="Demandeur">
              <div className="info-list">
                <Row label="Nom">{demandeur || null}</Row>
                <Row label="Téléphone">{invoice.telFidele}</Row>
                <Row label="E-mail">{invoice.emailFidele}</Row>
              </div>
            </AppCard>

            <AppCard
              title="Règlement"
              subtitle={paye ? 'Encaissement confirmé par l’agrégateur.' : 'Aucun encaissement confirmé.'}
            >
              {reglement ? (
                <div className="info-list">
                  <Row label="Moyen de paiement">
                    {[invoice.typePaiementLibelle, invoice.modePaiement].filter(Boolean).join(' · ') || null}
                  </Row>
                  <Row label="Opérateur">{reglement.provider}</Row>
                  <Row label="Numéro payeur">{reglement.numeroPayeur}</Row>
                  <Row label="Transaction">{reglement.idTransaction}</Row>
                  <Row label="Date">{reglement.datePaiement ? formatDate(reglement.datePaiement) : null}</Row>
                  <Row label="Statut">
                    <AppBadge
                      value={reglement.statut}
                      label={paymentStatusLabel(reglement.statut)}
                    />
                  </Row>
                </div>
              ) : (
                <p className="muted">
                  La facture n’a pas encore été réglée. Les frais et le net paroisse seront
                  connus dès la confirmation du paiement.
                </p>
              )}
            </AppCard>

            <AppCard
              title="Répartition du montant"
              subtitle="Du montant débité au fidèle jusqu’à la trésorerie de la paroisse."
            >
              {reglement ? (
                <div className="info-list">
                  <Row label="Débité au fidèle">
                    {reglement.montantCharge != null
                      ? formatCurrency(Number(reglement.montantCharge))
                      : formatCurrency(montant)}
                  </Row>
                  <Row label="Frais agrégateur">
                    {reglement.montantFraisAgregateur != null
                      ? `− ${formatCurrency(Number(reglement.montantFraisAgregateur))}`
                      : null}
                  </Row>
                  <Row label="Frais plateforme">
                    {reglement.montantFraisPlateforme != null
                      ? `− ${formatCurrency(Number(reglement.montantFraisPlateforme))}`
                      : null}
                  </Row>
                  <div className="info-row info-row-total">
                    <span>Net crédité à la paroisse</span>
                    <strong>
                      {reglement.montantNet != null
                        ? formatCurrency(Number(reglement.montantNet))
                        : '—'}
                    </strong>
                  </div>
                </div>
              ) : (
                <p className="muted">
                  Montant facturé : <strong>{formatCurrency(montant)}</strong>. La répartition
                  s’affichera après encaissement.
                </p>
              )}
            </AppCard>
          </div>
        </>
      ) : null}
    </div>
  );
}
