import React, { useEffect, useState } from 'react';
import { getApiBaseUrl } from '../../config/apiBaseUrl';
import AppButton from './AppButton';
import PdfPreviewModal from './PdfPreviewModal';

/**
 * Bouton « Aperçu du reçu » : charge le PDF en blob, affiche un aperçu, puis téléchargement.
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
  const [blobUrl, setBlobUrl] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => () => {
    if (blobUrl) URL.revokeObjectURL(blobUrl);
  }, [blobUrl]);

  if (!codeSuivie) return null;

  const fileName = `recu-${codeSuivie}.pdf`;
  const pdfUrl = `${getApiBaseUrl()}/demandes/code/${encodeURIComponent(codeSuivie)}/recu.pdf`;

  const openPreview = async () => {
    setLoading(true);
    setError(null);
    setOpen(true);
    try {
      const res = await fetch(pdfUrl, { headers: { Accept: 'application/pdf' } });
      if (!res.ok) {
        throw new Error('Impossible de charger le reçu PDF.');
      }
      const blob = await res.blob();
      if (blobUrl) URL.revokeObjectURL(blobUrl);
      setBlobUrl(URL.createObjectURL(blob));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Aperçu impossible');
      setBlobUrl(null);
    } finally {
      setLoading(false);
    }
  };

  const download = () => {
    if (!blobUrl) return;
    const anchor = document.createElement('a');
    anchor.href = blobUrl;
    anchor.download = fileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  };

  const close = () => {
    setOpen(false);
    setError(null);
  };

  return (
    <>
      {asLinkClassName ? (
        <button type="button" className={asLinkClassName} onClick={openPreview} disabled={loading}>
          {loading ? 'Chargement…' : label}
        </button>
      ) : (
        <AppButton
          type="button"
          variant={variant}
          size={size}
          className={className}
          loading={loading}
          onClick={openPreview}
        >
          {label}
        </AppButton>
      )}
      <PdfPreviewModal
        open={open}
        title="Aperçu du reçu"
        blobUrl={blobUrl}
        fileName={fileName}
        loading={loading}
        error={error}
        onClose={close}
        onDownload={download}
      />
    </>
  );
}
