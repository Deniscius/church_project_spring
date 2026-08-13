import React, { useMemo, useState } from 'react';
import { getReceiptPdfUrl, getReceiptPreviewUrl } from '../../utils/receiptPdfUrl';
import AppButton from './AppButton';
import PdfPreviewModal from './PdfPreviewModal';

/**
 * Aperçu reçu PDF.
 *
 * Le viewer reçoit directement l'URL de l'API au lieu d'une URL blob: intermédiaire.
 * Cela évite un second chargement blob par PDF.js et fonctionne aussi bien avec
 * le proxy Vite local qu'avec l'API HTTPS déployée sur Render.
 */
export default function ReceiptPreviewButton({
  codeSuivie,
  variant = 'secondary',
  size,
  className = '',
  label = 'Aperçu du reçu',
  asLinkClassName,
}) {
  const [open, setOpen] = useState(false);

  const previewUrl = useMemo(() => getReceiptPreviewUrl(codeSuivie), [codeSuivie]);
  const pdfUrl = useMemo(() => getReceiptPdfUrl(codeSuivie), [codeSuivie]);
  const fileName = useMemo(
    () => (codeSuivie ? `recu-${codeSuivie}.pdf` : 'recu.pdf'),
    [codeSuivie]
  );

  if (!codeSuivie || !previewUrl) return null;

  const openInTab = () => {
    window.open(pdfUrl || previewUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <>
      {asLinkClassName ? (
        <button type="button" className={asLinkClassName} onClick={() => setOpen(true)}>
          {label}
        </button>
      ) : (
        <AppButton
          type="button"
          variant={variant}
          size={size}
          className={className}
          onClick={() => setOpen(true)}
        >
          {label}
        </AppButton>
      )}

      <PdfPreviewModal
        open={open}
        title="Aperçu du reçu"
        pdfUrl={previewUrl}
        fileName={fileName}
        onClose={() => setOpen(false)}
        onDownload={openInTab}
        onOpenInTab={openInTab}
      />
    </>
  );
}
