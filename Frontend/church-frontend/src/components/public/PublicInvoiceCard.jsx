import React from 'react';
import AppCard from '../ui/AppCard';
import ReceiptPreviewButton from '../ui/ReceiptPreviewButton';

export default function PublicInvoiceCard({ codeSuivie }) {
  return (
    <AppCard
      title="Reçu PDF"
      subtitle="Aperçu et téléchargement du reçu lié à cette demande."
    >
      {codeSuivie ? (
        <>
          <p className="muted" style={{ marginTop: 0 }}>
            Consultez le reçu généré pour cette demande puis téléchargez-le si nécessaire.
          </p>
          <div className="button-row">
            <ReceiptPreviewButton
              codeSuivie={codeSuivie}
              variant="primary"
              label="Afficher le reçu PDF"
            />
          </div>
        </>
      ) : (
        <p className="muted" style={{ margin: 0 }}>
          Le reçu sera disponible dès que la facture aura été chargée.
        </p>
      )}
    </AppCard>
  );
}
