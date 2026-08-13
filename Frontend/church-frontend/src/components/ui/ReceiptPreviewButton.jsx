import React, { useEffect, useMemo, useState } from 'react';
import { getReceiptPdfUrl, getReceiptPreviewUrl } from '../../utils/receiptPdfUrl';
import { requestService } from '../../services/request.service';
import AppButton from './AppButton';
import PdfPreviewModal from './PdfPreviewModal';

async function blobLooksLikePdf(blob) {
  if (!(blob instanceof Blob) || blob.size < 5) return false;
  const head = new Uint8Array(await blob.slice(0, 4).arrayBuffer());
  return head[0] === 0x25 && head[1] === 0x50 && head[2] === 0x44 && head[3] === 0x46;
}

function humanizeReceiptError(err) {
  const raw = err instanceof Error ? err.message : String(err || '');
  if (/failed to fetch|networkerror|load failed|network request failed/i.test(raw)) {
    return 'Impossible de charger le reçu (réseau / tunnel). Essayez « Ouvrir dans un onglet ».';
  }
  return raw || 'Impossible de charger le reçu.';
}

/**
 * Aperçu reçu :
 * 1) fetch PDF → blob → rendu canvas (PDF.js) — prod Render / mobile
 * 2) fallback iframe same-origin /__receipt seulement si le fetch échoue (dev)
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

  const previewUrl = useMemo(() => getReceiptPreviewUrl(codeSuivie), [codeSuivie]);
  const pdfUrl = useMemo(() => getReceiptPdfUrl(codeSuivie), [codeSuivie]);
  const fileName = useMemo(
    () => (codeSuivie ? `recu-${codeSuivie}.pdf` : 'recu.pdf'),
    [codeSuivie]
  );
  const sameOriginPreview = Boolean(
    previewUrl && (previewUrl.startsWith('/__receipt') || previewUrl.startsWith('/api/'))
  );

  useEffect(() => () => {
    if (blobUrl) URL.revokeObjectURL(blobUrl);
  }, [blobUrl]);

  if (!codeSuivie || !previewUrl) return null;

  const openInTab = () => {
    window.open(pdfUrl || previewUrl, '_blank', 'noopener,noreferrer');
  };

  const openPreview = async () => {
    setOpen(true);
    setLoading(true);
    setError(null);
    try {
      const blob = await requestService.fetchReceiptPdf(codeSuivie);
      if (!(await blobLooksLikePdf(blob))) {
        throw new Error(
          'Réponse non-PDF (interstitiel ngrok ?). Ouvrez le site via « Visit Site » puis réessayez.'
        );
      }
      const next = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      if (blobUrl) URL.revokeObjectURL(blobUrl);
      setBlobUrl(next);
    } catch (err) {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
      setBlobUrl(null);
      if (sameOriginPreview) {
        // Iframe du PDF proxy — pas de fetch navigateur.
        setError(null);
        return;
      }
      setError(humanizeReceiptError(err));
    } finally {
      setLoading(false);
    }
  };

  const download = () => {
    if (blobUrl) {
      const anchor = document.createElement('a');
      anchor.href = blobUrl;
      anchor.download = fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      return;
    }
    openInTab();
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
        pdfUrl={!blobUrl && !error ? previewUrl : ''}
        fileName={fileName}
        loading={loading}
        error={error}
        onClose={() => {
          setOpen(false);
          setError(null);
        }}
        onDownload={download}
        onOpenInTab={openInTab}
      />
    </>
  );
}
