import React, { useEffect, useRef, useState } from 'react';
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist';
import pdfWorkerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url';

if (typeof window !== 'undefined') {
  GlobalWorkerOptions.workerSrc = pdfWorkerSrc;
}

async function resolvePdfData(source) {
  if (!source) return null;

  if (source instanceof ArrayBuffer) {
    return { data: source.slice(0) };
  }
  if (typeof Blob !== 'undefined' && source instanceof Blob) {
    return { data: await source.arrayBuffer() };
  }
  if (typeof source !== 'string') {
    throw new Error('Source PDF non supportée');
  }

  // blob: / data: — PDF.js charge via URL
  if (source.startsWith('blob:') || source.startsWith('data:')) {
    return { url: source };
  }

  // Same-origin (/__receipt, /api/…) ou URL absolue : fetch → ArrayBuffer
  // (évite iframe cross-origin / CSP).
  const absolute = /^https?:\/\//i.test(source);
  const res = await fetch(source, {
    credentials: absolute ? 'include' : 'same-origin',
    headers: {
      Accept: 'application/pdf',
      'ngrok-skip-browser-warning': 'true',
      'X-Requested-With': 'XMLHttpRequest',
    },
  });
  if (!res.ok) {
    throw new Error(`Impossible de charger le PDF (HTTP ${res.status})`);
  }
  return { data: await res.arrayBuffer() };
}

/**
 * Aperçu PDF fiable (canvas via PDF.js) — indépendant de l’iframe / plugin navigateur.
 * Fonctionne en prod cross-origin (blob) et sur mobile.
 */
export default function PdfCanvasViewer({ source, fileName = 'document.pdf' }) {
  const hostRef = useRef(null);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('Préparation de l’aperçu…');
  const [pageInfo, setPageInfo] = useState('');

  useEffect(() => {
    let cancelled = false;
    let pdfDoc = null;

    const render = async () => {
      setStatus('loading');
      setMessage('Préparation de l’aperçu…');
      setPageInfo('');
      const host = hostRef.current;
      if (!host) return;
      host.replaceChildren();

      if (!source) {
        setStatus('empty');
        setMessage('Aucun document à afficher.');
        return;
      }

      try {
        const docParams = await resolvePdfData(source);
        if (cancelled) return;
        if (!docParams) {
          setStatus('empty');
          setMessage('Aucun document à afficher.');
          return;
        }

        const loadingTask = getDocument({
          ...docParams,
          useSystemFonts: true,
        });
        pdfDoc = await loadingTask.promise;
        if (cancelled) return;

        const maxPages = Math.min(pdfDoc.numPages, 12);
        setPageInfo(
          pdfDoc.numPages > 1
            ? `${pdfDoc.numPages} page${pdfDoc.numPages > 1 ? 's' : ''}`
            : ''
        );

        for (let pageNum = 1; pageNum <= maxPages; pageNum += 1) {
          if (cancelled) break;
          const page = await pdfDoc.getPage(pageNum);
          const baseViewport = page.getViewport({ scale: 1 });
          const width = Math.max(host.clientWidth || 720, 280);
          const scale = Math.min(2.2, Math.max(1.1, (width - 16) / baseViewport.width));
          const viewport = page.getViewport({ scale });

          const canvas = document.createElement('canvas');
          canvas.className = 'pdf-preview-canvas';
          canvas.setAttribute('aria-label', `${fileName} — page ${pageNum}`);
          canvas.width = Math.floor(viewport.width);
          canvas.height = Math.floor(viewport.height);

          const ctx = canvas.getContext('2d', { alpha: false });
          await page.render({ canvasContext: ctx, viewport }).promise;
          if (!cancelled) host.appendChild(canvas);
        }

        if (cancelled) return;

        if (pdfDoc.numPages > maxPages) {
          const note = document.createElement('p');
          note.className = 'muted pdf-preview-more';
          note.textContent = `Aperçu des ${maxPages} premières pages — ouvrez dans un onglet pour le document complet.`;
          host.appendChild(note);
        }
        setStatus('ready');
        setMessage('');
      } catch (err) {
        if (cancelled) return;
        setStatus('error');
        setMessage(err instanceof Error ? err.message : 'Aperçu PDF impossible.');
      }
    };

    render();

    return () => {
      cancelled = true;
      if (pdfDoc) {
        try {
          pdfDoc.destroy();
        } catch {
          /* ignore */
        }
      }
    };
  }, [source, fileName]);

  return (
    <div className="pdf-canvas-viewer">
      {status === 'loading' ? <p className="muted">{message}</p> : null}
      {status === 'error' || status === 'empty' ? (
        <p
          className={status === 'error' ? 'text-red-600' : 'muted'}
          role={status === 'error' ? 'alert' : undefined}
        >
          {message}
          {status === 'error' ? ' Utilisez « Ouvrir dans un onglet » si besoin.' : ''}
        </p>
      ) : null}
      {pageInfo && status === 'ready' ? (
        <p className="muted pdf-preview-pageinfo">{pageInfo}</p>
      ) : null}
      <div ref={hostRef} className="pdf-preview-pages" />
    </div>
  );
}
