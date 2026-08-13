import React, { useEffect, useId } from 'react';
import AppButton from './AppButton';
import PdfCanvasViewer from './PdfCanvasViewer';

/**
 * Aperçu PDF (canvas PDF.js) + actions — fiable sur toute la plateforme.
 *
 * `pdfBlob` est privilégié : PDF.js lit directement les octets déjà récupérés
 * par apiClient, sans second fetch réseau et sans URL blob: intermédiaire.
 */
export default function PdfPreviewModal({
  open,
  title = 'Aperçu du PDF',
  pdfBlob,
  blobUrl,
  pdfUrl,
  fileName = 'document.pdf',
  loading = false,
  error = null,
  onClose,
  onDownload,
  onOpenInTab,
}) {
  const titleId = useId();
  const source = pdfBlob || blobUrl || pdfUrl || '';

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="pdf-preview-overlay" role="presentation" onClick={onClose}>
      <div
        className="pdf-preview-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(e) => e.stopPropagation()}
      >
        <header className="pdf-preview-head">
          <h2 id={titleId}>{title}</h2>
          <div className="button-row">
            {typeof onOpenInTab === 'function' ? (
              <AppButton type="button" variant="secondary" size="sm" onClick={onOpenInTab}>
                Ouvrir dans un onglet
              </AppButton>
            ) : null}
            <AppButton
              type="button"
              variant="primary"
              size="sm"
              disabled={loading || (!source && !onDownload)}
              onClick={onDownload}
            >
              Télécharger
            </AppButton>
            <AppButton type="button" variant="secondary" size="sm" onClick={onClose}>
              Fermer
            </AppButton>
          </div>
        </header>

        <div className="pdf-preview-body">
          {loading ? <p className="muted">Chargement du document…</p> : null}
          {error ? <p className="text-red-600" role="alert">{error}</p> : null}
          {!loading && !error && source ? (
            <PdfCanvasViewer source={source} fileName={fileName} />
          ) : null}
          {!loading && !error && !source ? (
            <p className="muted">Aucun document à afficher.</p>
          ) : null}
        </div>
      </div>
    </div>
  );
}
