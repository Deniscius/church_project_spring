import React, { useEffect, useId } from 'react';
import AppButton from './AppButton';

/**
 * Aperçu PDF avant téléchargement (iframe + actions).
 */
export default function PdfPreviewModal({
  open,
  title = 'Aperçu du PDF',
  blobUrl,
  fileName = 'document.pdf',
  loading = false,
  error = null,
  onClose,
  onDownload,
}) {
  const titleId = useId();

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
            <AppButton
              type="button"
              variant="primary"
              size="sm"
              disabled={!blobUrl || loading}
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
          {loading ? <p className="muted">Préparation de l’aperçu…</p> : null}
          {error ? <p className="text-red-600" role="alert">{error}</p> : null}
          {!loading && !error && blobUrl ? (
            <iframe title={fileName} src={blobUrl} className="pdf-preview-frame" />
          ) : null}
        </div>
      </div>
    </div>
  );
}
