import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

async function proxyReceiptPdf(code) {
  const encoded = encodeURIComponent(code);
  const headers = { Accept: 'application/pdf' };
  let upstream = await fetch(`http://127.0.0.1:8081/demandes/code/${encoded}/recu`, { headers });
  if (upstream.status === 404) {
    upstream = await fetch(`http://127.0.0.1:8081/demandes/code/${encoded}/recu.pdf`, { headers });
  }
  const buffer = Buffer.from(await upstream.arrayBuffer());
  return { upstream, buffer };
}

/**
 * GET /__receipt/:code → PDF (proxy Node → Spring).
 * À iframe / ouvrir dans un onglet : pas de fetch navigateur (fragile sous ngrok).
 */
function attachReceiptMiddleware(middlewares) {
  middlewares.use(async (req, res, next) => {
    const url = req.url || '';
    const pdfMatch = url.match(/^\/__receipt\/([^/?#]+)/);
    if (!pdfMatch) {
      next();
      return;
    }

    const code = decodeURIComponent(pdfMatch[1]);
    try {
      const { upstream, buffer } = await proxyReceiptPdf(code);
      if (!upstream.ok) {
        res.statusCode = upstream.status;
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        res.end(JSON.stringify({
          message: `Impossible de charger le reçu (HTTP ${upstream.status}).`,
        }));
        return;
      }
      res.statusCode = 200;
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="recu-${code}.pdf"`);
      res.setHeader('Cache-Control', 'no-store');
      res.setHeader('Content-Security-Policy', "frame-ancestors 'self'");
      res.end(buffer);
    } catch (err) {
      res.statusCode = 502;
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.end(JSON.stringify({
        message: 'Backend inaccessible pour le reçu PDF (port 8081).',
        detail: err instanceof Error ? err.message : String(err),
      }));
    }
  });
}

function receiptPdfDevProxy() {
  return {
    name: 'receipt-pdf-dev-proxy',
    configureServer(server) {
      attachReceiptMiddleware(server.middlewares);
    },
    configurePreviewServer(server) {
      attachReceiptMiddleware(server.middlewares);
    },
  };
}

const apiProxy = {
  '/api': {
    target: 'http://127.0.0.1:8081',
    changeOrigin: true,
    secure: false,
    rewrite: (path) => path.replace(/^\/api/, ''),
  },
};

export default defineConfig({
  plugins: [react(), receiptPdfDevProxy()],

  build: {
    target: 'es2020',
    cssCodeSplit: true,
    sourcemap: false,
    reportCompressedSize: true,
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (
            id.includes('react-dom')
            || id.includes('/react/')
            || id.includes('react-router')
            || id.includes('scheduler')
          ) {
            return 'vendor-react';
          }
          if (id.includes('@tanstack')) return 'vendor-query';
          if (id.includes('libphonenumber')) return 'vendor-phone';
          return 'vendor';
        },
      },
    },
  },

  server: {
    host: '0.0.0.0',
    allowedHosts: true,
    proxy: apiProxy,
  },

  // Build de prod servi localement (npm run prod:share) + tunnel
  preview: {
    host: '0.0.0.0',
    port: 4173,
    allowedHosts: true,
    proxy: apiProxy,
  },
});
