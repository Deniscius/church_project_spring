import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

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
    // Accepte les domaines ngrok (et autres tunnels) sans les figer dans le fichier
    allowedHosts: true,

    proxy: {
      // Un seul tunnel ngrok sur :5173 suffit : /api → backend :8081
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
