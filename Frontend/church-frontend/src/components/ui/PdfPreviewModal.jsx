import React, { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
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
  const dialogRef = useRef(null);
  const source = pdfBlob || blobUrl || pdfUrl || '';

  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const focusableSelector =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const focusId = window.requestAnimationFrame(() => {
      dialogRef.current?.querySelector(focusableSelector)?.focus();
    });

    const onKey = (event) => {
      if (event.key === 'Escape') {
        onClose?.();
        return;
      }
      if (event.key !== 'Tab') return;

      const focusable = [...(dialogRef.current?.querySelectorAll(focusableSelector) || [])];
      if (!focusable.length) {
        event.preventDefault();
        dialogRef.current?.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKey);
    return () => {
      window.cancelAnimationFrame(focusId);
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return createPortal(
    <div className="pdf-preview-overlay" role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
        tabIndex={-1}
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
    </div>,
    document.body
  );
}
