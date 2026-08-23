import React, { useEffect, useMemo, useRef, useState } from 'react';
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
    return 'Impossible de récupérer le reçu depuis l’API. Vérifiez la connexion et la configuration CORS de Render.';
  }
  return raw || 'Impossible de charger le reçu.';
}

/**
 * Aperçu reçu PDF :
 * 1) un seul fetch via apiClient ;
 * 2) le Blob reçu est donné directement à PDF.js ;
 * 3) aucun second fetch cross-origin et aucune URL blob: pour l’aperçu.
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
  const [pdfBlob, setPdfBlob] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const loadingRef = useRef(false);
  const abortRef = useRef(null);

  const previewUrl = useMemo(() => getReceiptPreviewUrl(codeSuivie), [codeSuivie]);
  const pdfUrl = useMemo(() => getReceiptPdfUrl(codeSuivie), [codeSuivie]);
  const fileName = useMemo(
    () => (codeSuivie ? `recu-${codeSuivie}.pdf` : 'recu.pdf'),
    [codeSuivie]
  );

  useEffect(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    loadingRef.current = false;
    setLoading(false);
    setPdfBlob(null);
    setError(null);
    setOpen(false);

    return () => abortRef.current?.abort();
  }, [codeSuivie]);

  if (!codeSuivie || !previewUrl) return null;

  const openInTab = () => {
    window.open(pdfUrl || previewUrl, '_blank', 'noopener,noreferrer');
  };

  const openPreview = async () => {
    setOpen(true);
    setError(null);

    if (pdfBlob || loadingRef.current) return;

    const controller = new AbortController();
    abortRef.current = controller;
    loadingRef.current = true;
    setLoading(true);
    try {
      const blob = await requestService.fetchReceiptPdf(
        codeSuivie,
        { signal: controller.signal }
      );
      if (!(await blobLooksLikePdf(blob))) {
        throw new Error('L’API n’a pas renvoyé un fichier PDF valide.');
      }
      if (!controller.signal.aborted) setPdfBlob(blob);
    } catch (err) {
      if (!controller.signal.aborted) {
        setPdfBlob(null);
        setError(humanizeReceiptError(err));
      }
    } finally {
      if (abortRef.current === controller) {
        abortRef.current = null;
        loadingRef.current = false;
        if (!controller.signal.aborted) setLoading(false);
      }
    }
  };

  const download = () => {
    if (!pdfBlob) {
      openInTab();
      return;
    }

    const objectUrl = URL.createObjectURL(pdfBlob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = fileName;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
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
        pdfBlob={pdfBlob}
        fileName={fileName}
        loading={loading}
        error={error}
        onClose={() => setOpen(false)}
        onDownload={download}
        onOpenInTab={openInTab}
      />
    </>
  );
}
